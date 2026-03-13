package main.kotlin.downloader

import main.kotlin.HttpClient

/**
 * Single-request downloader that fetches the full file body using regular GET.
 */
class SingleChunkFileDownloader(
    private val httpClient: HttpClient,
    private val url: String
) : FileDownloader {
    override fun download(): ByteArray {
        return httpClient.getFullBytes(url)
    }
}



