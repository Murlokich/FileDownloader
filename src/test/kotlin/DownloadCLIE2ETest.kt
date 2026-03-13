package test.kotlin

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.AfterClass
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class DownloadCLIE2ETest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun parallelHappyPath() {
        // Prepare temporary directory
        val outputDir = tempFolder.newFolder("downloads")
        val expectedBytes = payload
        val downloadUrl = "$baseUrl/files/sample.txt"
        val javaBin = "${System.getProperty("java.home")}/bin/java"
        val classpath = System.getProperty("java.class.path")

        // Run separate cli command process
        val process = ProcessBuilder(
            javaBin,
            "-cp",
            classpath,
            "main.kotlin.MainKt",
            "--chunks",
            "3",
            downloadUrl
        )
            .directory(outputDir)
            .redirectErrorStream(true)
            .start()

        val finished = process.waitFor(20, TimeUnit.SECONDS)
        val outputText = process.inputStream.bufferedReader().use { it.readText() }

        assertTrue("CLI process did not finish in time", finished)
        assertTrue("CLI process failed. Output:\n$outputText", process.exitValue() == 0)

        val outputFile = outputDir.toPath().resolve("sample.txt")
        assertTrue("Expected downloaded file to exist", Files.exists(outputFile))
        assertArrayEquals(expectedBytes, Files.readAllBytes(outputFile))
    }

    companion object {
        private lateinit var server: HttpServer
        private lateinit var baseUrl: String
        private lateinit var payload: ByteArray

        @JvmStatic
        @BeforeClass
        fun setUpServer() {
            // Serve sample.txt fixture
            payload = loadFixtureBytes()
            server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            server.createContext("/files/sample.txt") { exchange -> handleFileRequest(exchange) }
            server.start()
            baseUrl = "http://127.0.0.1:${server.address.port}"
        }

        @JvmStatic
        @AfterClass
        fun tearDownServer() {
            server.stop(0)
        }

        private fun handleFileRequest(exchange: HttpExchange) {
            when (exchange.requestMethod) {
                "HEAD" -> {
                    exchange.responseHeaders.add("Accept-Ranges", "bytes")
                    exchange.responseHeaders.add("Content-Length", payload.size.toString())
                    exchange.sendResponseHeaders(200, -1)
                    exchange.close()
                }

                "GET" -> {
                    val rangeHeader = exchange.requestHeaders.getFirst("Range")
                    val rangePrefix = "bytes="
                    if (rangeHeader == null || !rangeHeader.startsWith(rangePrefix)) {
                        exchange.sendResponseHeaders(416, -1)
                        exchange.close()
                        return
                    }

                    val parts = rangeHeader.removePrefix(rangePrefix).split("-", limit = 2)
                    val start = parts.getOrNull(0)?.toIntOrNull()
                    val end = parts.getOrNull(1)?.toIntOrNull()
                    if (start == null || end == null || start < 0 || end < start || end >= payload.size) {
                        exchange.sendResponseHeaders(416, -1)
                        exchange.close()
                        return
                    }

                    // Return exactly the requested inclusive range as HTTP 206 Partial Content.
                    val response = payload.copyOfRange(start, end + 1)
                    exchange.responseHeaders.add("Content-Range", "bytes $start-$end/${payload.size}")
                    exchange.sendResponseHeaders(206, response.size.toLong())
                    exchange.responseBody.use { it.write(response) }
                    exchange.close()
                }

                else -> {
                    exchange.sendResponseHeaders(405, -1)
                    exchange.close()
                }
            }
        }

        private fun loadFixtureBytes(): ByteArray {
            val stream = requireNotNull(DownloadCLIE2ETest::class.java.getResourceAsStream("/fixtures/sample.txt")) {
                "Missing test fixture: /fixtures/sample.txt"
            }
            return stream.use { it.readBytes() }
        }
    }
}




