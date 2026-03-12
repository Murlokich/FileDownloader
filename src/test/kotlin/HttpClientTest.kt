package test.kotlin

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import main.kotlin.ByteRange
import main.kotlin.HttpClient
import org.junit.AfterClass
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import kotlin.text.Charsets.UTF_8

class HttpClientTest {

    @Test
    fun getBytesInRangeReturnsRequestedBytes() {
        val result = client.getBytesInRange(baseUrl + "/range", ByteRange(2, 5))
        assertArrayEquals("cdef".toByteArray(UTF_8), result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getBytesInRangeRejectsNegativeLowerBound() {
        client.getBytesInRange(baseUrl + "/range", ByteRange(-1, 5))
    }

    @Test(expected = IllegalArgumentException::class)
    fun getBytesInRangeRejectsUpperBoundLessThanLowerBound() {
        client.getBytesInRange(baseUrl + "/range", ByteRange(10, 5))
    }

    @Test
    fun getBytesInRangeThrowsIOExceptionForUnsuccessfulResponse() {
        try {
            client.getBytesInRange(baseUrl + "/always404", ByteRange(0, 2))
        } catch (ex: IOException) {
            assertTrue(ex.message?.contains("HTTP 404") == true)
            return
        }

        throw AssertionError("Expected IOException to be thrown")
    }

    companion object {
        private lateinit var server: HttpServer
        private lateinit var baseUrl: String
        private val client = HttpClient()
        private val data = "abcdefghijklmnopqrstuvwxyz".toByteArray(UTF_8)

        @BeforeClass
        @JvmStatic
        fun setUpServer() {
            server = HttpServer.create(InetSocketAddress(0), 0)
            server.createContext("/range") { exchange ->
                handleRangeRequest(exchange)
            }
            server.createContext("/always404") { exchange ->
                exchange.sendResponseHeaders(404, -1)
                exchange.close()
            }
            server.start()
            baseUrl = "http://localhost:${server.address.port}"
        }

        @AfterClass
        @JvmStatic
        fun tearDownServer() {
            server.stop(0)
        }

        private fun handleRangeRequest(exchange: HttpExchange) {
            if (exchange.requestMethod != "GET") {
                exchange.sendResponseHeaders(405, -1)
                exchange.close()
                return
            }

            val rangeHeader = exchange.requestHeaders.getFirst("Range")
            val match = Regex("bytes=(\\d+)-(\\d+)").matchEntire(rangeHeader ?: "")

            if (match == null) {
                exchange.sendResponseHeaders(400, -1)
                exchange.close()
                return
            }

            val start = match.groupValues[1].toInt()
            val end = match.groupValues[2].toInt()

            if (start < 0 || end < start || end >= data.size) {
                exchange.sendResponseHeaders(416, -1)
                exchange.close()
                return
            }

            val response = data.copyOfRange(start, end + 1)
            exchange.responseHeaders.add("Content-Range", "bytes $start-$end/${data.size}")
            exchange.sendResponseHeaders(206, response.size.toLong())
            exchange.responseBody.use { out -> out.write(response) }
            exchange.close()
        }
    }
}

