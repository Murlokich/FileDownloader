/**
 * Simple CLI application for parallel file downloader.
 * Takes exactly one argument: the URL to download.
 */

fun main(args: Array<String>) {
    // Enforce exactly one argument
    if (args.size != 1) {
        println("Error: Expected exactly 1 argument, got ${args.size}")
        println("Usage: java -jar FileDownloader.jar <url>")
        println("Example: java -jar FileDownloader.jar https://example.com/file.zip")
        return
    }
    
    val url = args[0]
    println("Downloading: $url")
}

