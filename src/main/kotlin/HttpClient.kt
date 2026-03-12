package main.kotlin

import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException

/**
 * Exception thrown when the server does not support required capabilities
 * for downloading (e.g., byte ranges).
 */
class ServerCapabilityException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Simple HTTP client for making requests.
 */
class HttpClient {
    
    /**
     * Sends a HEAD request to the specified URL and retrieves the content length.
     * 
     * @param url The URL to request
     * @return Result containing content length in bytes on success, or an exception on failure
     * - IOException: if response is unsuccessful
     * - ServerCapabilityException: if server doesn't support byte ranges
     * - IllegalStateException: if content-length is invalid or missing
     */
    fun getContentLength(url: String): Result<Long> {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "HEAD"
                connection.connect()

                // Other status codes are not providing the required information
                if (connection.responseCode != 200) {
                    throw IOException(
                        "Failed to get required data from the server. HTTP " +
                                "${connection.responseCode}: ${connection.responseMessage}"
                    )
                }
                
                // If server doesn't support Range requests stop and report it
                // Done this way not to deceive user about inability for parallel downloading
                val acceptRanges = connection.getHeaderField("Accept-Ranges") ?: "none"
                if (acceptRanges.lowercase() == "none") {
                    throw ServerCapabilityException(
                        "Server does not support byte ranges. Accept-Ranges: $acceptRanges"
                    )
                }
                
                // Get content length
                val contentLength = connection.contentLengthLong
                if (contentLength <= 0) {
                    throw IllegalStateException(
                        "Server did not provide content-length or content-length is invalid: $contentLength"
                    )
                }
                
                Result.success(contentLength)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
