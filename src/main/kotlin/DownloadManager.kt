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
 * Manages file downloads from provided URLs.
 */
class DownloadManager(
    private var chunks: Int = NON_PARALLEL_CHUNKS
) {

    init {
        require(chunks > 0) { "chunks must be > 0" }
    }

    companion object {
        const val NON_PARALLEL_CHUNKS = 1
        const val PARALLEL_DOWNLOAD_SUPPORTED = true
        const val PARALLEL_DOWNLOAD_NOT_SUPPORTED = false
    }

    private val httpClient = HttpClient()

    /**
     * Checks whether downloading from this source is supported and in what mode
     *
     * @param headMeta Metadata returned from HEAD request
     * @return Whether server supports parallel (range-based) download
     * @throws IOException if HTTP status is not successful
     * @throws IllegalStateException if content length is invalid
     */
    fun processHeadMeta(headMeta: HeadMeta): Boolean {
        if (headMeta.statusCode != 200) {
            throw IOException(
                "Failed to get required data from the server. HTTP " +
                    "${headMeta.statusCode}: ${headMeta.statusMessage}"
            )
        }

        val acceptRanges = headMeta.acceptRanges?.trim()?.lowercase()
        if (acceptRanges != "bytes") {
            println(
                "Warning: server does not support byte ranges (Accept-Ranges: " +
                    "${headMeta.acceptRanges ?: "<missing>"}). Falling back to single-chunk download."
            )
            return PARALLEL_DOWNLOAD_NOT_SUPPORTED
        }

        if (headMeta.contentLength <= 0) {
            throw IllegalStateException(
                "Server did not provide content-length or content-length is invalid: ${headMeta.contentLength}"
            )
        }

        return PARALLEL_DOWNLOAD_SUPPORTED
    }

    /**
     * Initiates a download for the specified URL.
     *
     * @param url The URL to download from
     */
    fun downloadFile(url: String) {
        println("Downloading from: $url")

        val headMeta = httpClient.getHeadMeta(url)
        val isParallelDownloadSupported = processHeadMeta(headMeta)
        val contentLength = headMeta.contentLength

        if (!isParallelDownloadSupported) {
            chunks = NON_PARALLEL_CHUNKS
        }
        chunks = minOf(chunks.toLong(), contentLength).toInt()

        val downloadedBytes = if (chunks == NON_PARALLEL_CHUNKS) {
            httpClient.getFullBytes(url)
        } else {
            val ranges = splitIntoRanges(contentLength, chunks)
            val bytes = ByteArray(contentLength.toInt())
            downloadChunks(url, ranges, bytes)
            bytes
        }

        val outputPath = resolveOutputPath(url)
        Files.write(outputPath, downloadedBytes)

        println("Source is valid for download")
        println("Content-Length: ${headMeta.contentLength} bytes")
        println("Downloaded ${downloadedBytes.size} bytes in $chunks chunks")
        println("Saved to: ${outputPath.toAbsolutePath()}")
    }

    private fun resolveOutputPath(url: String): Path {
        val fileName = URI(url).path.substringAfterLast('/').ifBlank { "download.bin" }
        return Paths.get(fileName)
    }

    fun splitIntoRanges(contentLength: Long): Array<ByteRange> {
        return splitIntoRanges(contentLength, chunks)
    }

    private fun splitIntoRanges(contentLength: Long, chunkCount: Int): Array<ByteRange> {
        require(contentLength > 0) { "contentLength must be > 0" }
        require(contentLength >= chunkCount) {
            "contentLength must be >= chunkCount to produce non-empty ranges"
        }

        val chunkSize = contentLength / chunkCount
        val ranges = Array(chunkCount) { ByteRange(0, 0) }
        var start = 0L

        for (index in 0 until chunkCount) {
            val end = if (index == chunkCount - 1) {
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

