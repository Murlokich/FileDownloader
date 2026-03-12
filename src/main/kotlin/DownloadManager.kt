package main.kotlin

import java.io.IOException

/**
 * Thrown when server metadata does not satisfy downloader requirements.
 */
class ServerCapabilityException(message: String) : Exception(message)

/**
 * Manages file downloads from provided URLs.
 */
class DownloadManager {

    private val httpClient = HttpClient()

    /**
     * Validates whether downloading from this source is supported.
     *
     * @param headMeta Metadata returned from HEAD request
     * @throws IOException if HTTP status is not successful
     * @throws ServerCapabilityException if byte ranges are not supported
     * @throws IllegalStateException if content length is invalid
     */
    fun validateHeadMeta(headMeta: HeadMeta) {
        if (headMeta.statusCode != 200) {
            throw IOException(
                "Failed to get required data from the server. HTTP " +
                    "${headMeta.statusCode}: ${headMeta.statusMessage}"
            )
        }

        val acceptRanges = headMeta.acceptRanges?.trim()?.lowercase()
        if (acceptRanges != "bytes") {
            throw ServerCapabilityException(
                "Server does not support byte ranges. Accept-Ranges: ${headMeta.acceptRanges ?: "<missing>"}"
            )
        }

        if (headMeta.contentLength <= 0) {
            throw IllegalStateException(
                "Server did not provide content-length or content-length is invalid: ${headMeta.contentLength}"
            )
        }
    }

    /**
     * Initiates a download for the specified URL.
     *
     * @param url The URL to download from
     */
    fun downloadFile(url: String) {
        println("Downloading from: $url")

        try {
            val headMeta = httpClient.getHeadMeta(url)
            validateHeadMeta(headMeta)

            println("Source is valid for download")
            println("Content-Length: ${headMeta.contentLength} bytes")
        } catch (error: Exception) {
            println("Error: ${error.message}")
        }
    }
}
