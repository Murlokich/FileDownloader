package main.kotlin

import java.net.HttpURLConnection
import java.net.URI

/**
 * Immutable snapshot of response metadata returned by a HEAD request.
 */
data class HeadMeta(
    val statusCode: Int,
    val statusMessage: String?,
    val acceptRanges: String?,
    val contentLength: Long
)

/**
 * Simple HTTP client for making requests.
 */
class HttpClient {

    /**
     * Sends a HEAD request and returns response metadata.
     *
     * @param url The URL to request
     * @return HeadMeta extracted from response headers/status
     * @throws Exception if request creation or execution fails
     */
    fun getHeadMeta(url: String): HeadMeta {
        var connection: HttpURLConnection? = null

        try {
            connection = URI(url).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connect()

            return HeadMeta(
                statusCode = connection.responseCode,
                statusMessage = connection.responseMessage,
                acceptRanges = connection.getHeaderField("Accept-Ranges"),
                contentLength = connection.contentLengthLong
            )
        } finally {
            connection?.disconnect()
        }
    }
}
