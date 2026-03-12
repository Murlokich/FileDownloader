package main.kotlin

/**
 * Manages file downloads from provided URLs.
 */
class DownloadManager {
    
    private val httpClient = HttpClient()
    
    /**
     * Initiates a download for the specified URL.
     * 
     * @param url The URL to download from
     */
    fun downloadFile(url: String) {
        println("Downloading from: $url")
        
        try {
            val contentLength = httpClient.getContentLength(url)
            println("Content-Length: $contentLength bytes")
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }
}
