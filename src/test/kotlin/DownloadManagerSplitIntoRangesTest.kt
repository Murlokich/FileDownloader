package test.kotlin

import main.kotlin.ByteRange
import main.kotlin.DownloadManager
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class DownloadManagerSplitIntoRangesTest {

    @Test
    fun splitIntoRangesWhenDivisible() {
        val manager = DownloadManager()

        val ranges = manager.splitIntoRanges(contentLength = 12)

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
        val manager = DownloadManager(chunks = 4)

        val ranges = manager.splitIntoRanges(contentLength = 10)

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
        val manager = DownloadManager()
        manager.splitIntoRanges(contentLength = 2)
    }
}

