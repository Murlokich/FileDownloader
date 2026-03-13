package main.kotlin

import main.kotlin.downloader.FileDownloader
import main.kotlin.downloader.ParallelFileDownloader
import main.kotlin.downloader.SingleChunkFileDownloader
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

        val chunksToUse = minOf(chunks.toLong(), contentLength).toInt()

        val downloader: FileDownloader = if (chunksToUse == NON_PARALLEL_CHUNKS) {
            SingleChunkFileDownloader(httpClient, url)
        } else {
            ParallelFileDownloader(httpClient, url, chunksToUse, contentLength)
        }

        val downloadedBytes = downloader.download()

        val outputPath = resolveOutputPath(url)
        Files.write(outputPath, downloadedBytes)

        println("Source is valid for download")
        println("Content-Length: ${headMeta.contentLength} bytes")
        println("Downloaded ${downloadedBytes.size} bytes in $chunksToUse chunks")
        println("Saved to: ${outputPath.toAbsolutePath()}")
    }

    private fun resolveOutputPath(url: String): Path {
        val fileName = URI(url).path.substringAfterLast('/').ifBlank { "download.bin" }
        return Paths.get(fileName)
    }
}

