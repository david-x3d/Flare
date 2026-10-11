package dev.flare.speedtest.data.network

import dev.flare.speedtest.domain.SpeedTestError
import dev.flare.speedtest.domain.SpeedTestException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Executes [request] on the IO dispatcher and runs [block] with the response and the time the call
 * started (so that `System.nanoTime() - startNanos` inside [block] is the wait for the headers). OkHttp's blocking
 * calls cannot be interrupted by coroutine cancellation, so the call is cancelled explicitly the
 * moment the coroutine is; the resulting `IOException("Canceled")` is turned back into a proper
 * [CancellationException].
 */
internal suspend fun <T> OkHttpClient.executeCancellable(
    request: Request,
    callTimeoutMillis: Long = 0L,
    block: (response: Response, startNanos: Long) -> T,
): T = withContext(Dispatchers.IO) {
    val call = newCall(request)
    if (callTimeoutMillis > 0) call.timeout().timeout(callTimeoutMillis, TimeUnit.MILLISECONDS)
    coroutineScope {
        // execute() blocks and ignores coroutine cancellation, so a watcher cancels the call for it.
        val watcher = launch {
            try {
                awaitCancellation()
            } finally {
                call.cancel()
            }
        }
        try {
            val startNanos = System.nanoTime()
            call.execute().use { block(it, startNanos) }
        } catch (e: IOException) {
            ensureActive()
            throw e
        } finally {
            watcher.cancel()
        }
    }
}

/** Translates any failure of a run into a [SpeedTestException]; cancellation is passed through. */
internal fun Throwable.toSpeedTestException(host: String): Throwable = when (this) {
    is CancellationException -> this
    is SpeedTestException -> this
    is UnknownHostException, is ConnectException, is NoRouteToHostException ->
        SpeedTestException(SpeedTestError.Unreachable(host), this)

    is SocketTimeoutException, is InterruptedIOException -> SpeedTestException(SpeedTestError.Timeout, this)
    else -> SpeedTestException(SpeedTestError.Unknown(message), this)
}

