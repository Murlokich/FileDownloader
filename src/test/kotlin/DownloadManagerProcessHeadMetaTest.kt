package test.kotlin

import main.kotlin.DownloadManager
import main.kotlin.HeadMeta
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class DownloadManagerProcessHeadMetaTest {

    @Test
    fun processHeadMetaAcceptsValidMetadata() {
        val manager = DownloadManager()
        val meta = HeadMeta(
            statusCode = 200,
            statusMessage = "OK",
            acceptRanges = "bytes",
            contentLength = 42
        )

        val isParallelSupported = manager.processHeadMeta(meta)
        assertEquals(DownloadManager.PARALLEL_DOWNLOAD_SUPPORTED, isParallelSupported)
    }

    @Test(expected = IOException::class)
    fun processHeadMetaRejectsNonSuccessStatus() {
        val manager = DownloadManager()
        val meta = HeadMeta(
            statusCode = 404,
            statusMessage = "Not Found",
            acceptRanges = "bytes",
            contentLength = -1
        )

        manager.processHeadMeta(meta)
    }

    @Test
    fun processHeadMetaFallsBackToSingleChunkWhenByteRangesUnsupported() {
        val manager = DownloadManager()
        val meta = HeadMeta(
            statusCode = 200,
            statusMessage = "OK",
            acceptRanges = "none",
            contentLength = 42
        )

        val isParallelSupported = manager.processHeadMeta(meta)
        assertEquals(DownloadManager.PARALLEL_DOWNLOAD_NOT_SUPPORTED, isParallelSupported)
    }

    @Test(expected = IllegalStateException::class)
    fun processHeadMetaRejectsNonPositiveContentLength() {
        val manager = DownloadManager()
        val meta = HeadMeta(
            statusCode = 200,
            statusMessage = "OK",
            acceptRanges = "bytes",
            contentLength = 0
        )

        manager.processHeadMeta(meta)
    }
}

