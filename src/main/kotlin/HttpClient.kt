package main.kotlin

import java.net.HttpURLConnection
import java.net.URI
import java.io.IOException

/**
 * Inclusive byte range used for ranged requests.
 */
data class ByteRange(
    val start: Long,
    val end: Long
)

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

    /**
     * Sends a ranged GET request and returns fetched bytes.
     *
     * @param url The URL to request
     * @param range Inclusive byte range to request
     * @return Bytes returned by the server for the requested range
     * @throws IllegalArgumentException if range bounds are invalid
     * @throws IOException if server response is unsuccessful
     */
    fun getBytesInRange(url: String, range: ByteRange): ByteArray {
        require(range.start >= 0) { "range.start must be >= 0" }
        require(range.end >= 0) { "range.end must be >= 0" }
        require(range.end >= range.start) { "range.end must be >= range.start" }

        var connection: HttpURLConnection? = null

        try {
            connection = URI(url).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Range", "bytes=${range.start}-${range.end}")
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode !in setOf(200, 206)) {
                throw IOException("GET range request failed with HTTP $responseCode ${connection.responseMessage}")
            }

            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection?.disconnect()
        }
    }
}
