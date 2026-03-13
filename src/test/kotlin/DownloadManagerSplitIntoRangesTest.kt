package test.kotlin

import main.kotlin.ByteRange
import main.kotlin.HttpClient
import main.kotlin.downloader.ParallelFileDownloader
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class DownloadManagerSplitIntoRangesTest {

    @Test
    fun splitIntoRangesWhenDivisible() {
        val downloader = ParallelFileDownloader(HttpClient(), url = "http://example.com/file", chunks = 3, contentLength = 12)

        val ranges = downloader.splitIntoRanges(contentLength = 12)

        assertArrayEquals(
            arrayOf(
                ByteRange(0, 3),
                ByteRange(4, 7),
                ByteRange(8, 11)
            ),
            ranges
        )
    }

    @Test
    fun splitIntoRangesWhenNotDivisible() {
        val downloader = ParallelFileDownloader(HttpClient(), url = "http://example.com/file", chunks = 4, contentLength = 10)

        val ranges = downloader.splitIntoRanges(contentLength = 10)

        assertArrayEquals(
            arrayOf(
                ByteRange(0, 1),
                ByteRange(2, 3),
                ByteRange(4, 5),
                ByteRange(6, 9)
            ),
            ranges
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun splitIntoRangesRejectsContentLengthLessThanChunks() {
        val downloader = ParallelFileDownloader(HttpClient(), url = "http://example.com/file", chunks = 3, contentLength = 2)
        downloader.splitIntoRanges(contentLength = 2)
    }
}

