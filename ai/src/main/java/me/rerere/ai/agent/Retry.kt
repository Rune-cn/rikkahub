package me.rerere.ai.agent

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.io.IOException
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 重试等待的间隔策略：
 *
 * - [FIXED]：固定间隔，每次都等待 [RetryPolicy.initialDelay]
 * - [EXPONENTIAL_BACKOFF]：指数退避，等待时间从 [RetryPolicy.initialDelay] 开始每次翻倍（1s、2s、4s…）
 * - [JITTER]：Full Jitter，在 `[1ms, 指数退避值]` 内随机，避免大批请求同时重试造成"重试风暴"（限流场景更平稳）
 */
enum class RetryBackoff {
    FIXED,
    EXPONENTIAL_BACKOFF,
    JITTER,
}

/**
 * 请求模型失败时的重试策略：最多重试 [maxRetries] 次，等待时间由 [backoff] 策略决定（默认指数退避）。
 *
 * 默认只重试 [IOException]，也就是连不上、超时、读到一半断开这类网络故障。
 */
data class RetryPolicy(
    val maxRetries: Int,
    val initialDelay: Duration = 1.seconds,
    val backoff: RetryBackoff = RetryBackoff.EXPONENTIAL_BACKOFF,
    val retryOn: (Throwable) -> Boolean = { it is IOException },
) {
    companion object {
        val NONE = RetryPolicy(maxRetries = 0)

        /** 网络自动重试：最多 3 次，间隔 1s、2s、4s */
        val NETWORK = RetryPolicy(maxRetries = 3)
    }
}

/** 第 [retry] 次重试（共 [maxRetries] 次）将在 [delay] 后开始，[cause] 是上一次失败的原因 */
data class RetryAttempt(
    val cause: Throwable,
    val retry: Int,
    val maxRetries: Int,
    val delay: Duration,
)

/**
 * 按 [policy] 执行 [block]：失败且允许重试时先调用 [onRetry]，等待后再执行一次。
 */
suspend fun <T> withRetry(
    policy: RetryPolicy,
    onRetry: suspend (RetryAttempt) -> Unit = {},
    block: suspend () -> T,
): T {
    var retries = 0
    while (true) {
        try {
            return block()
        } catch (error: Throwable) {
            // 用户主动停止生成时，底层连接也可能以 IOException("canceled") 收尾；
            // 先检查协程状态，确保取消不会被当作网络波动重新拉起。
            currentCoroutineContext().ensureActive()
            if (error is CancellationException || retries >= policy.maxRetries || !policy.retryOn(error)) {
                throw error
            }
            retries++
            val exponential = policy.initialDelay * (1 shl (retries - 1))
            val wait = when (policy.backoff) {
                RetryBackoff.FIXED -> policy.initialDelay
                RetryBackoff.EXPONENTIAL_BACKOFF -> exponential
                RetryBackoff.JITTER -> {
                    val capMs = exponential.inWholeMilliseconds.coerceAtLeast(1)
                    Random.nextLong(1, capMs + 1).milliseconds
                }
            }
            onRetry(RetryAttempt(error, retries, policy.maxRetries, wait))
            delay(wait)
        }
    }
}
