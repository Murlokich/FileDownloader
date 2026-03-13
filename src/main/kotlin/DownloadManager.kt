package main.kotlin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Thrown when server metadata does not satisfy downloader requirements.
 */
class ServerCapabilityException(message: String) : Exception(message)


/**
 * Manages file downloads from provided URLs.
 */
class DownloadManager(
    private val chunks: Int = DEFAULT_CHUNKS
) {

    init {
        require(chunks > 0) { "chunks must be > 0" }
    }

    companion object {
        private const val DEFAULT_CHUNKS = 3
    }

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
     * Splits [contentLength] into [chunks] inclusive byte ranges.
     *
     * The last range absorbs the remainder so the final end is always contentLength - 1.
     */
    fun splitIntoRanges(contentLength: Long): Array<ByteRange> {
        require(contentLength > 0) { "contentLength must be > 0" }
        require(contentLength >= chunks) {
            "contentLength must be >= chunks to produce non-empty ranges"
        }

        val chunkSize = contentLength / chunks
        val ranges = Array(chunks) { ByteRange(0, 0) }
        var start = 0L

        for (index in 0 until chunks) {
            val end = if (index == chunks - 1) {
                contentLength - 1
            } else {
                start + chunkSize - 1
            }

            ranges[index] = ByteRange(start = start, end = end)
            start = end + 1
        }

        return ranges
    }

    /**
     * Initiates a download for the specified URL.
     *
     * @param url The URL to download from
     */
    fun downloadFile(url: String) {
        println("Downloading from: $url")

        val headMeta = httpClient.getHeadMeta(url)
        validateHeadMeta(headMeta)
        val contentLength = headMeta.contentLength

        val ranges = splitIntoRanges(contentLength)
        val downloadedBytes = ByteArray(contentLength.toInt())

        downloadChunks(url, ranges, downloadedBytes)

        val outputPath = resolveOutputPath(url)
        Files.write(outputPath, downloadedBytes)

        println("Source is valid for download")
        println("Content-Length: ${headMeta.contentLength} bytes")
        println("Downloaded ${downloadedBytes.size} bytes in ${ranges.size} chunks")
        println("Saved to: ${outputPath.toAbsolutePath()}")
    }

    private fun resolveOutputPath(url: String): Path {
        val fileName = URI(url).path.substringAfterLast('/').ifBlank { "download.bin" }
        return Paths.get(fileName)
    }

    /**
     * Downloads all [ranges] concurrently and writes each chunk directly into [destination].
     */
    private fun downloadChunks(url: String, ranges: Array<ByteRange>, destination: ByteArray) = runBlocking {
        val chunkJobs = ranges.map { range ->
            async(Dispatchers.IO) {
                val chunkBytes = httpClient.getBytesInRange(url, range)
                val expectedChunkSize = (range.end - range.start + 1).toInt()
                if (chunkBytes.size != expectedChunkSize) {
                    throw IllegalStateException(
                        "Range ${range.start}-${range.end} returned ${chunkBytes.size} bytes " +
                            "instead of $expectedChunkSize"
                    )
                }

                System.arraycopy(
                    chunkBytes,
                    0,
                    destination,
                    range.start.toInt(),
                    chunkBytes.size
                )
            }
        }

        chunkJobs.awaitAll()
    }
}

