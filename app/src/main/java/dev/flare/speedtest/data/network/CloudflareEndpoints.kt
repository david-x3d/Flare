package dev.flare.speedtest.data.network

/**
 * The measurement endpoints of speed.cloudflare.com.
 *
 * These are the endpoints Cloudflare's own speed test web page uses. They are **not** documented as
 * a stable public API and may change or disappear without notice. Everything that depends on their
 * shape lives in this file and in [CloudflareSpeedTestEngine]; when Cloudflare changes something,
 * the app reports [dev.flare.speedtest.domain.SpeedTestError.EndpointChanged] instead of crashing.
 */
object CloudflareEndpoints {
    /** Change this single constant to point Flare at another host. */
    const val BASE_URL = "https://speed.cloudflare.com"

    /** `GET /__down?bytes=N` streams N bytes; `bytes=0` answers with headers only (latency probe). */
    const val DOWNLOAD_PATH = "__down"
    const val DOWNLOAD_QUERY_BYTES = "bytes"

    /** `POST /__up` swallows the request body and answers 200 with an empty body. */
    const val UPLOAD_PATH = "__up"

    /** `GET /meta` returns provider, client IP and the serving colo as JSON. */
    const val META_PATH = "meta"
}
