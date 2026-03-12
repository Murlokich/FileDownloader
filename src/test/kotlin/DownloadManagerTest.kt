package test.kotlin

import main.kotlin.ByteRange
import main.kotlin.DownloadManager
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class DownloadManagerTest {

    @Test
    fun splitIntoRangesWhenDivisible() {
        val manager = DownloadManager(chunks = 3)

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
        val manager = DownloadManager(chunks = 3)

        val ranges = manager.splitIntoRanges(contentLength = 10)

        assertArrayEquals(
            arrayOf(
                ByteRange(0, 2),
                ByteRange(3, 5),
                ByteRange(6, 9)
            ),
            ranges
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun splitIntoRangesRejectsContentLengthLessThanChunks() {
        val manager = DownloadManager(chunks = 3)
        manager.splitIntoRanges(contentLength = 2)
    }
}

