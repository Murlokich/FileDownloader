package test.kotlin

import main.kotlin.DownloadManager
import main.kotlin.HeadMeta
import main.kotlin.ServerCapabilityException
import org.junit.Test
import java.io.IOException

class DownloadManagerValidateHeadMetaTest {

    @Test
    fun validateHeadMetaAcceptsValidMetadata() {
        val manager = DownloadManager()
        val meta = HeadMeta(
            statusCode = 200,
            statusMessage = "OK",
            acceptRanges = "bytes",
            contentLength = 42
        )

        manager.validateHeadMeta(meta)
    }

    @Test(expected = IOException::class)
    fun validateHeadMetaRejectsNonSuccessStatus() {
        val manager = DownloadManager()
        val meta = HeadMeta(
            statusCode = 404,
            statusMessage = "Not Found",
            acceptRanges = "bytes",
            contentLength = -1
        )

        manager.validateHeadMeta(meta)
    }

    @Test(expected = ServerCapabilityException::class)
    fun validateHeadMetaRejectsMissingByteRangesSupport() {
        val manager = DownloadManager()
        val meta = HeadMeta(
            statusCode = 200,
            statusMessage = "OK",
            acceptRanges = "none",
            contentLength = 42
        )

        manager.validateHeadMeta(meta)
    }

    @Test(expected = IllegalStateException::class)
    fun validateHeadMetaRejectsNonPositiveContentLength() {
        val manager = DownloadManager()
        val meta = HeadMeta(
            statusCode = 200,
            statusMessage = "OK",
            acceptRanges = "bytes",
            contentLength = 0
        )

        manager.validateHeadMeta(meta)
    }
}

