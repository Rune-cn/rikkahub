package me.rerere.ai.agent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RetryTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `io failures are retried with doubling delays`() = runTest {
        var calls = 0
        val attempts = mutableListOf<RetryAttempt>()

        val result = withRetry(RetryPolicy.NETWORK, onRetry = { attempts += it }) {
            calls++
            if (calls < 3) throw IOException("timeout")
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(listOf(1, 2), attempts.map { it.retry })
        assertEquals(listOf(1.seconds, 2.seconds), attempts.map { it.delay })
        assertEquals(3_000L, currentTime)
    }

    @Test
    fun `gives up after max retries`() = runTest {
        var calls = 0

        val error = runCatching {
            withRetry(RetryPolicy.NETWORK) {
                calls++
                throw IOException("offline")
            }
        }.exceptionOrNull()

        assertTrue(error is IOException)
        assertEquals(4, calls)
    }

    @Test
    fun `other errors are thrown without retrying`() = runTest {
        var calls = 0

        val error = runCatching {
            withRetry(RetryPolicy.NETWORK) {
                calls++
                throw IllegalStateException("bad request")
            }
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals(1, calls)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `fixed backoff keeps the same delay`() = runTest {
        var calls = 0
        val attempts = mutableListOf<RetryAttempt>()

        val result = withRetry(
            RetryPolicy(maxRetries = 3, initialDelay = 2.seconds, backoff = RetryBackoff.FIXED),
            onRetry = { attempts += it },
        ) {
            calls++
            if (calls < 3) throw IOException("timeout")
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(listOf(2.seconds, 2.seconds), attempts.map { it.delay })
        assertEquals(4_000L, currentTime)
    }

    @Test
    fun `jitter backoff waits within the exponential cap`() = runTest {
        var calls = 0
        val attempts = mutableListOf<RetryAttempt>()

        withRetry(
            RetryPolicy(maxRetries = 2, initialDelay = 1.seconds, backoff = RetryBackoff.JITTER),
            onRetry = { attempts += it },
        ) {
            calls++
            if (calls < 3) throw IOException("timeout")
            "ok"
        }

        // 第 1 次重试：Full Jitter，在 [1ms, 1s] 内随机；第 2 次：在 [1ms, 2s] 内随机
        assertEquals(2, attempts.size)
        val first = attempts[0].delay
        assertTrue(first > Duration.ZERO && first <= 1.seconds)
        val second = attempts[1].delay
        assertTrue(second > Duration.ZERO && second <= 2.seconds)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `io failure caused by cancellation is not retried`() = runTest {
        var calls = 0

        val job = launch {
            withRetry(RetryPolicy(maxRetries = 3, initialDelay = Duration.ZERO)) {
                calls++
                // 停止生成时，底层连接会以 IOException("Canceled") 结束请求
                currentCoroutineContext().cancel()
                throw IOException("Canceled")
            }
        }
        job.join()

        assertTrue(job.isCancelled)
        assertEquals(1, calls)
    }
}
