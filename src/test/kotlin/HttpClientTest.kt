package test.kotlin

import com.sun.net.httpserver.HttpServer
import main.kotlin.ByteRange
import main.kotlin.HttpClient
import org.junit.AfterClass
import org.junit.Assert.assertArrayEquals
import org.junit.BeforeClass
import org.junit.Test
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

class HttpClientTest {

    @Test
    fun getFullBytesReturnsAllBytes() {
        val bytes = client.getFullBytes("$baseUrl/file.txt")

        assertArrayEquals(payload, bytes)
    }

    @Test
    fun getBytesInRangeReturnsRequestedPart() {
        val bytes = client.getBytesInRange("$baseUrl/file.txt", ByteRange(0, 4))

        assertArrayEquals("hello".toByteArray(StandardCharsets.UTF_8), bytes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getBytesInRangeRejectsInvalidRange() {
        client.getBytesInRange("$baseUrl/file.txt", ByteRange(-1, 1))
    }

    companion object {
        private lateinit var server: HttpServer
        private lateinit var baseUrl: String
        private val client = HttpClient()
        private val payload = "hello world".toByteArray(StandardCharsets.UTF_8)

        @JvmStatic
        @BeforeClass
        fun setUp() {
            server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            server.createContext("/file.txt") { exchange ->
                when (exchange.requestMethod) {
                    "GET" -> {
                        val rangeHeader = exchange.requestHeaders.getFirst("Range")
                        if (rangeHeader == null) {
                            exchange.sendResponseHeaders(200, payload.size.toLong())
                            exchange.responseBody.use { it.write(payload) }
                        } else {
                            val parts = rangeHeader.removePrefix("bytes=").split("-", limit = 2)
                            val start = parts[0].toInt()
                            val end = parts[1].toInt()
                            val chunk = payload.copyOfRange(start, end + 1)
                            exchange.sendResponseHeaders(206, chunk.size.toLong())
                            exchange.responseBody.use { it.write(chunk) }
                        }
                    }

                    else -> exchange.sendResponseHeaders(405, -1)
                }
                exchange.close()
            }
            server.start()
            baseUrl = "http://127.0.0.1:${server.address.port}"
        }

        @JvmStatic
        @AfterClass
        fun tearDown() {
            server.stop(0)
        }
    }
}

