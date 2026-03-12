package test.kotlin

import main.kotlin.isValidUrl
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

class UrlValidatorTest {
    @Test
    fun `valid HTTPS URL`() {
        assertTrue(isValidUrl("https://example.com/file.zip"))
    }
    @Test
    fun `valid HTTP URL`() {
        assertTrue(isValidUrl("http://example.com/file.zip"))
    }
    @Test
    fun `valid HTTPS URL with query parameters`() {
        assertTrue(isValidUrl("https://example.com/file.zip?key=value"))
    }
    @Test
    fun `valid HTTPS URL with custom port`() {
        assertTrue(isValidUrl("https://example.com:8080/file.zip"))
    }
    @Test
    fun `invalid URL - missing protocol`() {
        assertFalse(isValidUrl("example.com/file.zip"))
    }
    @Test
    fun `invalid URL - FTP protocol`() {
        assertFalse(isValidUrl("ftp://example.com/file.zip"))
    }
    @Test
    fun `invalid URL - completely malformed`() {
        assertFalse(isValidUrl("not a url"))
    }
    @Test
    fun `invalid URL - empty string`() {
        assertFalse(isValidUrl(""))
    }
    @Test
    fun `invalid URL - only protocol`() {
        assertFalse(isValidUrl("https://"))
    }
    @Test
    fun `invalid URL - FILE protocol`() {
        assertFalse(isValidUrl("file:///home/user/file.zip"))
    }
}
