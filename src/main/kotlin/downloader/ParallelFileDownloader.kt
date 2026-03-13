package main.kotlin.downloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import main.kotlin.ByteRange
import main.kotlin.HttpClient

/**
 * Parallel downloader that splits content into ranges and fetches them concurrently.
 */
class ParallelFileDownloader(
    private val httpClient: HttpClient,
    private val url: String,
    private val chunks: Int,
    private val contentLength: Long
) : FileDownloader {

    init {
        require(chunks > 1) { "chunks must be > 1 for parallel download" }
        require(contentLength > 0) { "contentLength must be > 0" }
        require(contentLength >= chunks) {
            "contentLength must be >= chunks to produce non-empty ranges"
        }
    }

    override fun download(): ByteArray {
        val ranges = splitIntoRanges(contentLength)
        val bytes = ByteArray(contentLength.toInt())
        downloadChunks(ranges, bytes)
        return bytes
    }

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

    private fun downloadChunks(ranges: Array<ByteRange>, destination: ByteArray) = runBlocking {
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



