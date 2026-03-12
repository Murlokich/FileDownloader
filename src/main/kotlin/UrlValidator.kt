package main.kotlin

import java.net.URI

/**
 * Validates if a string is a valid HTTP/HTTPS URL.
 */
fun isValidUrl(urlString: String): Boolean {
    return try {
        val url = URI(urlString).toURL()
        val protocol = url.protocol.lowercase()
        protocol == "http" || protocol == "https"
    } catch (e: Exception) {
        false
    }
}
