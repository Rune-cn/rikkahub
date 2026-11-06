package me.rerere.rikkahub.data.ai.mcp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.rerere.oauth.OAuthAuthorizationLauncher
import me.rerere.oauth.OAuthHttpClient
import me.rerere.oauth.OAuthLoopbackCallbackServer
import me.rerere.rikkahub.AppScope
import me.rerere.rikkahub.data.datastore.SettingsStore
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

private const val TAG = "McpOAuthCoordinator"
private const val TOKEN_REFRESH_LEEWAY_MS = 60_000L
internal const val MCP_OAUTH_CALLBACK_PORT = 52_134
internal const val MCP_OAUTH_CALLBACK_PATH = "/oauth/callback"

/**
 * 动态注册时按顺序尝试的 loopback redirect URI。
 *
 * 先试 `localhost`：多数授权服务器接受它，且部分服务器前置的 WAF 会拦截
 * 请求体里带 IPv4 字面地址的 URL。
 * 再退回 `127.0.0.1`：RFC 8252 把两者都算 loopback，但有服务器（如 cxyonly.fans）
 * 只接受 IP 形式，对 `localhost` 返回 invalid_redirect_uri。
 */
internal val MCP_OAUTH_REDIRECT_URI_CANDIDATES = listOf(
    "http://localhost:$MCP_OAUTH_CALLBACK_PORT$MCP_OAUTH_CALLBACK_PATH",
    "http://127.0.0.1:$MCP_OAUTH_CALLBACK_PORT$MCP_OAUTH_CALLBACK_PATH",
)

/** 默认 redirect URI（localhost）。Client ID Metadata Document 声明的也是这一个。 */
internal const val MCP_OAUTH_REDIRECT_URI =
    "http://localhost:$MCP_OAUTH_CALLBACK_PORT$MCP_OAUTH_CALLBACK_PATH"
// 托管在 GitHub Pages 上的 Client ID Metadata Document，其 redirect_uris 必须与 MCP_OAUTH_REDIRECT_URI 一致
internal const val MCP_OAUTH_CLIENT_METADATA_URL = "https://rikkahub.github.io/oauth/client.json"
private val OAUTH_CALLBACK_TIMEOUT = 5.minutes

/**
 * 负责 MCP OAuth 的授权、令牌刷新与持久化。
 *
 * 连接生命周期由配置流的消费者管理；令牌持久化后，配置变化会自然触发连接替换。
 */
internal class McpOAuthCoordinator(
    private val settingsStore: SettingsStore,
    private val appScope: AppScope,
    private val oauthClient: OAuthHttpClient,
    private val discoveryClient: McpOAuthDiscoveryClient,
    private val callbackServer: OAuthLoopbackCallbackServer,
    private val authorizationLauncher: OAuthAuthorizationLauncher,
    private val updateStatus: (Uuid, McpStatus) -> Unit,
) {
    private val authorizationJobs = ConcurrentHashMap<Uuid, Job>()
    private val refreshLocks = ConcurrentHashMap<Uuid, Mutex>()

    fun startAuthorization(config: McpServerConfig, context: Context) {
        authorizationJobs.remove(config.id)?.cancel()
        val job = appScope.launch {
            updateStatus(config.id, McpStatus.Authorizing)
            try {
                authorize(config, context.applicationContext)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "OAuth authorization failed for ${config.commonOptions.name}", e)
                updateStatus(config.id, McpStatus.Error.from(e, fallbackMessage = "OAuth authorization failed"))
            }
        }
        authorizationJobs[config.id] = job
        job.invokeOnCompletion { authorizationJobs.remove(config.id, job) }
    }

    fun cancelAuthorization(configId: Uuid) {
        authorizationJobs.remove(configId)?.cancel()
        updateStatus(configId, McpStatus.NeedsAuthorization)
    }

    fun forget(configId: Uuid) {
        authorizationJobs.remove(configId)?.cancel()
        refreshLocks.remove(configId)
    }

    suspend fun clearAuthorization(config: McpServerConfig): McpServerConfig {
        persistOAuthState(config.id, null)
        return settingsStore.settingsFlow.value.mcpServers.find { it.id == config.id }
            ?: config.clone(commonOptions = config.commonOptions.copy(oauth = null))
    }

    /**
     * 按 serverId 串行刷新。获得锁后重新读取配置，避免并发工具调用重复使用同一个 refresh token。
     */
    suspend fun ensureFreshToken(configInput: McpServerConfig): McpServerConfig {
        val lock = refreshLocks.computeIfAbsent(configInput.id) { Mutex() }
        return lock.withLock {
            val config = settingsStore.settingsFlow.value.mcpServers.find { it.id == configInput.id }
                ?: configInput
            val oauth = config.commonOptions.oauth ?: return@withLock config
            if (!oauth.enabled || oauth.refreshToken.isNullOrBlank()) return@withLock config

            val expired = oauth.expiresAt > 0 &&
                System.currentTimeMillis() >= oauth.expiresAt - TOKEN_REFRESH_LEEWAY_MS
            if (!oauth.accessToken.isNullOrBlank() && !expired) return@withLock config

            val tokenEndpoint = oauth.tokenEndpoint ?: return@withLock config
            val clientId = oauth.clientId ?: return@withLock config
            runCatching {
                val token = oauthClient.refreshToken(
                    OAuthHttpClient.RefreshTokenRequest(
                        tokenEndpoint = tokenEndpoint,
                        clientId = clientId,
                        clientSecret = oauth.clientSecret,
                        refreshToken = oauth.refreshToken,
                        resources = listOf(McpOAuthDiscoveryClient.canonicalResource(config.serverUrl)),
                        scope = oauth.scope,
                    )
                )
                val updated = oauth.copy(
                    accessToken = token.accessToken,
                    refreshToken = token.refreshToken ?: oauth.refreshToken,
                    expiresAt = computeExpiry(token.expiresIn),
                    scope = token.scope ?: oauth.scope,
                )
                persistOAuthState(config.id, updated)
                config.clone(commonOptions = config.commonOptions.copy(oauth = updated))
            }.getOrElse {
                Log.w(TAG, "Token refresh failed for ${config.commonOptions.name}: ${it.message}")
                config
            }
        }
    }

    suspend fun needsAuthorization(config: McpServerConfig, error: Throwable): Boolean {
        if (config.commonOptions.headers.any { it.first.equals("Authorization", ignoreCase = true) }) {
            return false
        }
        if (looksUnauthorized(error)) return true
        return runCatching { discoveryClient.discoverProtectedResource(config.serverUrl) }
            .onFailure {
                Log.i(TAG, "OAuth probe failed for ${config.commonOptions.name}: ${it.message}")
            }
            .isSuccess
    }

    private suspend fun authorize(config: McpServerConfig, context: Context) = withContext(Dispatchers.IO) {
        val serverUrl = config.serverUrl
        require(serverUrl.isNotBlank()) { "Server URL 为空，无法授权" }

        // 部分服务器（如 Zomato）不提供 RFC 9728 元数据，按旧版规范退回到服务器 origin 作为授权服务器
        val protectedResource = runCatching { discoveryClient.discoverProtectedResource(serverUrl) }
            .onFailure { Log.i(TAG, "Protected resource discovery failed, fallback to server origin: ${it.message}") }
            .getOrNull()
        val issuer = protectedResource?.authorizationServers?.firstOrNull()
            ?: McpOAuthDiscoveryClient.serverOrigin(serverUrl)
            ?: error("无法确定授权服务器")
        val metadata = discoveryClient.discoverAuthorizationServer(issuer)
        val authorizationEndpoint = metadata.authorizationEndpoint
            ?: error("授权服务器缺少 authorization_endpoint")
        val tokenEndpoint = metadata.tokenEndpoint
            ?: error("授权服务器缺少 token_endpoint")
        val scope = config.commonOptions.oauth?.scope
            ?: protectedResource?.scopesSupported?.joinToString(" ")
            ?: metadata.scopesSupported?.joinToString(" ")

        val pkce = oauthClient.generatePkce()
        val state = oauthClient.generateState()
        val resource = McpOAuthDiscoveryClient.canonicalResource(serverUrl)
        val callbackSession = callbackServer.openSession(context, state)
        try {
            // 回调服务器由本进程启动，主机名只可能是 localhost / 127.0.0.1 之一。
            // 不比对固定常量：两种写法都可能出现，且部分授权服务器只接受其中之一。
            val callbackRedirectUri = callbackSession.redirectUri
            check(callbackRedirectUri in MCP_OAUTH_REDIRECT_URI_CANDIDATES) {
                "OAuth 回调服务器地址不在预期范围内: $callbackRedirectUri"
            }
            val existing = config.commonOptions.oauth
            val canReuseClient =
                existing?.redirectUri == callbackRedirectUri && !existing.clientId.isNullOrBlank()
            var clientId = existing?.clientId.takeIf { canReuseClient }
            var clientSecret = existing?.clientSecret.takeIf { canReuseClient }
            if (clientId.isNullOrBlank() && metadata.registrationEndpoint == null &&
                metadata.clientIdMetadataDocumentSupported
            ) {
                // Client ID Metadata Document 里的 redirect_uris 只有 localhost。若回调服务器
                // 声明的是 127.0.0.1，这个 client_id 与 redirect_uri 不匹配，授权会被拒；
                // 此时不采用它，交给下面的动态注册按候选顺序处理。
                if (callbackRedirectUri == MCP_OAUTH_REDIRECT_URI) {
                    clientId = MCP_OAUTH_CLIENT_METADATA_URL
                    clientSecret = null
                } else {
                    Log.i(
                        TAG,
                        "跳过 Client ID Metadata Document：其 redirect_uris 仅含 localhost，" +
                            "当前回调为 $callbackRedirectUri",
                    )
                }
            }
            // 注册成功后必须以「服务器实际接受的」redirect_uri 构造后续请求，
            // 否则授权请求与 token 请求里的 redirect_uri 会和注册值不一致而失败。
            var redirectUri = callbackRedirectUri
            if (clientId.isNullOrBlank()) {
                val registrationEndpoint = metadata.registrationEndpoint
                    ?: error("授权服务器不支持动态注册，且未预配置 client_id")
                val (registration, acceptedRedirectUri) =
                    oauthClient.registerClientTryingRedirectUris(
                        registrationEndpoint = registrationEndpoint,
                        request = OAuthHttpClient.ClientRegistrationRequest(
                            clientName = config.commonOptions.name.ifBlank { "RikkaHub" },
                            redirectUris = listOf(callbackRedirectUri),
                            scope = scope,
                        ),
                        redirectUris = MCP_OAUTH_REDIRECT_URI_CANDIDATES,
                    )
                clientId = registration.clientId
                clientSecret = registration.clientSecret
                redirectUri = acceptedRedirectUri
                Log.i(
                    TAG,
                    "动态注册成功: ${config.commonOptions.name} redirect_uri=$acceptedRedirectUri",
                )
            }

            persistOAuthState(
                config.id,
                (existing ?: McpOAuthState()).copy(
                    enabled = true,
                    clientId = clientId,
                    clientSecret = clientSecret,
                    authorizationEndpoint = authorizationEndpoint,
                    tokenEndpoint = tokenEndpoint,
                    registrationEndpoint = metadata.registrationEndpoint,
                    redirectUri = redirectUri,
                    scope = scope,
                )
            )

            val authorizationUrl = oauthClient.buildAuthorizationUrl(
                OAuthHttpClient.AuthorizationRequest(
                    authorizationEndpoint = authorizationEndpoint,
                    clientId = clientId,
                    redirectUri = redirectUri,
                    pkce = pkce,
                    state = state,
                    scope = scope,
                    resources = listOf(resource),
                )
            )
            withContext(Dispatchers.Main) {
                authorizationLauncher.launch(context, authorizationUrl)
            }

            val callback = callbackSession.awaitCallback(OAUTH_CALLBACK_TIMEOUT)
                ?: error("OAuth 授权超时")
            callback.error?.let { error(buildAuthorizationError(it, callback.errorDescription)) }
            val code = callback.code ?: error("授权失败: 未返回授权码")

            val token = oauthClient.exchangeAuthorizationCode(
                OAuthHttpClient.AuthorizationCodeTokenRequest(
                    tokenEndpoint = tokenEndpoint,
                    clientId = clientId,
                    clientSecret = clientSecret,
                    code = code,
                    codeVerifier = pkce.verifier,
                    redirectUri = redirectUri,
                    resources = listOf(resource),
                )
            )
            persistOAuthState(
                config.id,
                McpOAuthState(
                    enabled = true,
                    clientId = clientId,
                    clientSecret = clientSecret,
                    authorizationEndpoint = authorizationEndpoint,
                    tokenEndpoint = tokenEndpoint,
                    registrationEndpoint = metadata.registrationEndpoint,
                    redirectUri = redirectUri,
                    scope = token.scope ?: scope,
                    accessToken = token.accessToken,
                    refreshToken = token.refreshToken,
                    expiresAt = computeExpiry(token.expiresIn),
                )
            )
        } finally {
            withContext(NonCancellable) {
                callbackSession.close()
            }
        }
    }

    private fun buildAuthorizationError(error: String, description: String?): String =
        if (description.isNullOrBlank()) "授权失败: $error" else "授权失败: $error ($description)"

    private suspend fun persistOAuthState(configId: Uuid, oauth: McpOAuthState?) {
        settingsStore.update { old ->
            old.copy(
                mcpServers = old.mcpServers.map { server ->
                    if (server.id != configId) server
                    else server.clone(commonOptions = server.commonOptions.copy(oauth = oauth))
                }
            )
        }
    }

    private fun computeExpiry(expiresIn: Long?): Long =
        if (expiresIn != null && expiresIn > 0) {
            System.currentTimeMillis() + expiresIn * 1000
        } else {
            0L
        }

    private fun looksUnauthorized(error: Throwable): Boolean {
        val message = generateSequence(error) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" ")
            .lowercase()
        return message.contains("401") ||
            message.contains("unauthorized") ||
            message.contains("invalid_token") ||
            message.contains("invalid access token") ||
            message.contains("missing or invalid")
    }
}

