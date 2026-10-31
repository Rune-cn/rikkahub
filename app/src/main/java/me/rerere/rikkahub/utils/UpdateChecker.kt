package me.rerere.rikkahub.utils

import me.rerere.rikkahub.BuildConfig

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.rerere.common.http.await
import me.rerere.rikkahub.AppScope
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class UpdateChecker(
    private val context: Context,
    private val client: OkHttpClient,
    appScope: AppScope,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val updateState: StateFlow<UiState<UpdateInfo>> = checkUpdate().stateIn(
        scope = appScope,
        started = SharingStarted.Lazily,
        initialValue = UiState.Loading,
    )

    /**
     * 手动触发一次更新检查并返回最终结果（供「关于页 → 检查更新」使用；
     * 返回前会完整跑完检查流程，不依赖 updateState 的共享缓存）。
     */
    suspend fun checkNowOnce(): UiState<UpdateInfo> = checkUpdate().last()

    /**
     * 检查本仓库（fork）的最新**正式版**（stable）Release 是否有更新。
     * 不再依赖上游 updates.rikka-ai.com 服务；对比依据为版本号（remote > local 才提示）。
     *
     * 两套方案（2026-10-31 实测）：
     * 1. GitHub API `releases/latest`：数据最全（版本名/发布时间/更新日志/资产直链）；
     *    但匿名调用受 60 次/小时配额限制（设备共用 IP 容易踩到 403）。
     * 2. GitHub 网页 `releases/latest` 的 302 重定向：无 API 配额限制，
     *    从 Location 头拿最新正式版 tag；资产名按当前命名规范拼直链，HEAD 不通则退到发布页。
     * API 失败（限流/网络异常）时自动降级到方案 2。
     */
    @OptIn(ExperimentalTime::class)
    private fun checkUpdate(): Flow<UiState<UpdateInfo>> = flow<UiState<UpdateInfo>> {
        emit(UiState.Loading)
        RunLog.log("UpdateCheck", "开始检查正式版更新（local=${BuildConfig.VERSION_NAME}）")
        // 方案一：GitHub API releases/latest
        val apiRelease = runCatching { fetchLatestReleaseViaApi() }.getOrNull()
        if (apiRelease != null) {
            emit(buildUpdateState(
                remoteName = apiRelease.name,
                publishedAt = apiRelease.publishedAt,
                changelog = apiRelease.body,
                prerelease = apiRelease.prerelease,
                downloads = apiRelease.assets.map { asset ->
                    UpdateDownload(
                        name = asset.name,
                        url = asset.browserDownloadUrl,
                        size = formatSize(asset.size),
                    )
                },
            ))
            return@flow
        }
        RunLog.log("UpdateCheck", "API 方案不可用，降级到网页 302 重定向", 'W')
        // 方案二：GitHub 网页 302 重定向（无 API 配额限制），只拿最新 tag 版本
        val tag = runCatching { fetchLatestTagViaRedirect() }.getOrNull()
        if (tag != null) {
            emit(buildUpdateStateFromTag(tag))
            return@flow
        }
        RunLog.log("UpdateCheck", "两个方案都失败，本次不提示", 'W')
        emit(UiState.Idle)
    }.catch {
        // 网络异常时静默，避免打扰用户
        RunLog.log("UpdateCheck", "检查更新异常: ${it.message}", 'E')
        emit(UiState.Idle)
    }.flowOn(Dispatchers.IO)

    /** 拉取 releases/latest（失败返回 null，由调用方降级） */
    private suspend fun fetchLatestReleaseViaApi(): GitHubRelease? {
        val request = Request.Builder()
            .url("https://api.github.com/repos/Rune-cn/rikkahub/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "RikkaHub")
            .build()
        return try {
            val response = client.newCall(request).await()
            if (!response.isSuccessful) {
                RunLog.log("UpdateCheck", "API releases/latest 不可用: HTTP ${response.code}", 'W')
                null
            } else {
                json.decodeFromString<GitHubRelease>(response.body?.string().orEmpty())
                    .also { release ->
                        RunLog.log("UpdateCheck", "API 成功: ${release.name} (prerelease=${release.prerelease}, assets=${release.assets.size})")
                    }
            }
        } catch (e: Exception) {
            RunLog.log("UpdateCheck", "API 请求异常: ${e.message}", 'W')
            null
        }
    }

    /** 通过 releases/latest 的 302 重定向拿最新正式版 tag（如 2.5.7），免 API 配额 */
    private suspend fun fetchLatestTagViaRedirect(): String? {
        val request = Request.Builder()
            .url("https://github.com/Rune-cn/rikkahub/releases/latest")
            .header("User-Agent", "RikkaHub")
            .build()
        return try {
            val response = client.newBuilder().followRedirects(false).build().newCall(request).await()
            // 302/301 都是重定向（3xx 不算 isSuccessful），关键在 Location 头
            val location = response.header("Location").orEmpty()
            if (location.isBlank()) {
                RunLog.log("UpdateCheck", "302 响应无 Location 头（HTTP ${response.code}）", 'W')
                return null
            }
            // Location 示例：https://github.com/Rune-cn/rikkahub/releases/tag/2.5.7
            val tag = location.substringAfterLast('/').trim().takeIf {
                it.isNotBlank() && it.all { c -> c.isLetterOrDigit() || c == '.' || c == '-' || c == '_' }
            }
            RunLog.log("UpdateCheck", "302 重定向: $location -> tag=$tag")
            tag
        } catch (e: Exception) {
            RunLog.log("UpdateCheck", "302 重定向请求异常: ${e.message}", 'W')
            null
        }
    }

    private fun buildUpdateState(
        remoteName: String,
        publishedAt: String,
        changelog: String,
        prerelease: Boolean,
        downloads: List<UpdateDownload>,
    ): UiState<UpdateInfo> {
        // 检查正式版：仅当远端版本号高于本地版本才提示
        val remoteVersion = remoteName.trim().substringBefore('-')
        val localVersion = BuildConfig.VERSION_NAME.substringBefore('-')
        if (remoteVersion.isBlank() || Version(remoteVersion) <= Version(localVersion)) {
            RunLog.log("UpdateCheck", "API 版本比较: remote=$remoteVersion vs local=$localVersion -> 不提示")
            return UiState.Idle
        }
        RunLog.log("UpdateCheck", "API 版本比较: remote=$remoteVersion > local=$localVersion -> 提示更新")
        return UiState.Success(
            UpdateInfo(
                // 版本号格式：应用版本-构建 commit（如 2.5.6-a3ea1cd1）
                version = remoteName.ifBlank { publishedAt.take(10).replace("-", ".") },
                publishedAt = publishedAt,
                changelog = changelog,
                prerelease = prerelease,
                downloads = downloads,
            )
        )
    }

    /** 302 兜底：只有 tag，资产名按当前命名规范拼接直链；HEAD 不通则退到发布页 */
    private suspend fun buildUpdateStateFromTag(tag: String): UiState<UpdateInfo> {
        val localVersion = BuildConfig.VERSION_NAME.substringBefore('-')
        if (tag.isBlank() || Version(tag) <= Version(localVersion)) {
            RunLog.log("UpdateCheck", "302 版本比较: remote=$tag vs local=$localVersion -> 不提示")
            return UiState.Idle
        }
        RunLog.log("UpdateCheck", "302 版本比较: remote=$tag > local=$localVersion -> 提示更新")
        val apkUrl = "https://github.com/Rune-cn/rikkahub/releases/download/$tag/app-github-arm64-v8a-release.apk"
        val (name, url) = if (headReachable(apkUrl)) {
            RunLog.log("UpdateCheck", "直链探测可用: $apkUrl")
            "app-github-arm64-v8a-release.apk" to apkUrl
        } else {
            // 旧版命名（如 app-arm64-v8a-release.apk）没有该直链，退到发布页让用户自行选择
            RunLog.log("UpdateCheck", "直链探测不可用，退到发布页: $apkUrl", 'W')
            "Release $tag" to "https://github.com/Rune-cn/rikkahub/releases/tag/$tag"
        }
        return UiState.Success(
            UpdateInfo(
                version = tag,
                publishedAt = "",
                changelog = "",
                prerelease = false,
                downloads = listOf(UpdateDownload(name, url, "")),
            )
        )
    }

    /** 探测直链是否可下载（跟随重定向后应为 200） */
    private suspend fun headReachable(url: String): Boolean = runCatching {
        val response = client.newCall(
            Request.Builder().url(url).method("HEAD", null).header("User-Agent", "RikkaHub").build()
        ).await()
        response.isSuccessful
    }.getOrDefault(false)

    private fun formatSize(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
        bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }

    fun downloadUpdate(context: Context, download: UpdateDownload) {
        runCatching {
            val request = DownloadManager.Request(download.url.toUri()).apply {
                // 设置下载时通知栏的标题和描述
                setTitle(download.name)
                setDescription("正在下载更新包...")
                // 下载完成后通知栏可见
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                // 允许在移动网络和WiFi下下载
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                // 设置文件保存路径
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, download.name)
                // 允许下载的文件类型
                setMimeType("application/vnd.android.package-archive")
            }
            // 获取系统的DownloadManager
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            // 你可以保存返回的downloadId到本地，以便后续查询下载进度或状态
        }.onFailure {
            Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show()
            context.openUrl(download.url) // 跳转到下载页面
        }
    }
}

@Serializable
data class UpdateDownload(
    val name: String,
    val url: String,
    val size: String
)

@Serializable
data class UpdateInfo(
    val version: String,
    val publishedAt: String,
    val changelog: String,
    val downloads: List<UpdateDownload>,
    // 是否为预发布（测试版）Release
    val prerelease: Boolean = false,
)

/** GitHub Release API 响应（仅取所需字段） */
@Serializable
private data class GitHubRelease(
    @SerialName("name") val name: String = "",
    @SerialName("published_at") val publishedAt: String = "",
    @SerialName("body") val body: String = "",
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("assets") val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
private data class GitHubAsset(
    @SerialName("name") val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    @SerialName("size") val size: Long = 0,
)

/**
 * 版本号值类，封装版本号字符串并提供比较功能
 *
 * 支持完整的 SemVer 规范：MAJOR.MINOR.PATCH[-prerelease][+build]
 * - 预发布版本优先级低于正式版：1.0.0-alpha < 1.0.0
 * - 预发布标识符按段逐个比较：数字按数值比较，字符串按字典序比较
 * - 预发布标识符优先级：alpha < beta < rc（通过字典序自然满足）
 * - build metadata（+号后面的部分）不影响优先级比较
 */
@JvmInline
value class Version(val value: String) : Comparable<Version> {

    private fun parse(): ParsedVersion {
        // 去掉 build metadata（+号后面的部分）
        val withoutBuild = value.split("+").first()
        // 分离主版本号和预发布标识符
        val hyphenIndex = withoutBuild.indexOf('-')
        val (coreStr, prereleaseStr) = if (hyphenIndex >= 0) {
            withoutBuild.substring(0, hyphenIndex) to withoutBuild.substring(hyphenIndex + 1)
        } else {
            withoutBuild to null
        }
        val core = coreStr.split(".").map { it.toIntOrNull() ?: 0 }
        val prerelease = prereleaseStr?.split(".")
        return ParsedVersion(core, prerelease)
    }

    override fun compareTo(other: Version): Int {
        val a = this.parse()
        val b = other.parse()

        // 先比较主版本号
        val maxLen = maxOf(a.core.size, b.core.size)
        for (i in 0 until maxLen) {
            val ap = if (i < a.core.size) a.core[i] else 0
            val bp = if (i < b.core.size) b.core[i] else 0
            if (ap != bp) return ap.compareTo(bp)
        }

        // 主版本号相同时比较预发布标识符
        // 有预发布标识符的版本优先级低于没有的：1.0.0-alpha < 1.0.0
        return when {
            a.prerelease == null && b.prerelease == null -> 0
            a.prerelease != null && b.prerelease == null -> -1
            a.prerelease == null && b.prerelease != null -> 1
            else -> comparePrerelease(a.prerelease!!, b.prerelease!!)
        }
    }

    companion object {
        fun compare(version1: String, version2: String): Int {
            return Version(version1).compareTo(Version(version2))
        }

        private fun comparePrerelease(a: List<String>, b: List<String>): Int {
            val maxLen = maxOf(a.size, b.size)
            for (i in 0 until maxLen) {
                // 字段少的优先级更低：1.0.0-alpha < 1.0.0-alpha.1
                if (i >= a.size) return -1
                if (i >= b.size) return 1

                val aNum = a[i].toIntOrNull()
                val bNum = b[i].toIntOrNull()

                val cmp = when {
                    // 都是字：按数值比较
                    aNum != null && bNum != null -> aNum.compareTo(bNum)
                    // 数字优先级低于字符串
                    aNum != null -> -1
                    bNum != null -> 1
                    // 都是字符串：按字典序比较
                    else -> a[i].compareTo(b[i])
                }
                if (cmp != 0) return cmp
            }
            return 0
        }
    }
}

private data class ParsedVersion(
    val core: List<Int>,
    val prerelease: List<String>?,
)

// 扩展操作符函数，使比较更直观
operator fun String.compareTo(other: Version): Int = Version(this).compareTo(other)
operator fun Version.compareTo(other: String): Int = this.compareTo(Version(other))
