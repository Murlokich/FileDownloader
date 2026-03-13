package main.kotlin.downloader

/**
 * Strategy for downloading file bytes from a URL.
 */
interface FileDownloader {
    fun download(): ByteArray
}



