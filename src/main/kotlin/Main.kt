package main.kotlin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int

private class DownloadCommand : CliktCommand(
    name = "FileDownloader",
    help = "Parallel file downloader"
) {
    private val chunks by option("-c", "--chunks", help = "Number of chunks for parallel download")
        .int()
        .default(DownloadManager.DEFAULT_CHUNKS)
        .validate { value -> require(value > 0) { "Chunks must be a positive integer" } }

    private val url by argument(help = "URL to download")

    override fun run() {
        if (!isValidUrl(url)) {
            echo("Error: Invalid URL or unsupported protocol")
            echo("URL must start with http:// or https://")
            echo("You provided: $url")
            return
        }

        try {
            DownloadManager(chunks = chunks).downloadFile(url)
        } catch (error: Exception) {
            echo("Error: ${error.message}")
        }
    }
}

/**
 * Simple CLI application for parallel file downloader.
 * Accepts exactly one URL and optional chunk count.
 */
fun main(args: Array<String>) {
    DownloadCommand().main(args)
}
