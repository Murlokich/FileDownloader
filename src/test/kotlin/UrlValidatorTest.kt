package test.kotlin

import main.kotlin.isValidUrl
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

class UrlValidatorTest {
    @Test
    fun validHttpsUrl() {
        assertTrue(isValidUrl("https://example.com/file.zip"))
    }
    @Test
    fun validHttpUrl() {
        assertTrue(isValidUrl("http://example.com/file.zip"))
    }
    @Test
    fun validUrlWithQueryParams() {
        assertTrue(isValidUrl("https://example.com/file.zip?key=value"))
    }
    @Test
    fun validUrlWithCustomPort() {
        assertTrue(isValidUrl("https://example.com:8080/file.zip"))
    }
    @Test
    fun invalidMissingProtocol() {
        assertFalse(isValidUrl("example.com/file.zip"))
    }
    @Test
    fun invalidFtpProtocol() {
        assertFalse(isValidUrl("ftp://example.com/file.zip"))
    }
    @Test
    fun invalidMalformedUrl() {
        assertFalse(isValidUrl("not a url"))
    }
    @Test
    fun invalidEmptyString() {
        assertFalse(isValidUrl(""))
    }
    @Test
    fun invalidOnlyProtocol() {
        assertFalse(isValidUrl("https://"))
    }
    @Test
    fun invalidFileProtocol() {
        assertFalse(isValidUrl("file:///home/user/file.zip"))
    }
}
