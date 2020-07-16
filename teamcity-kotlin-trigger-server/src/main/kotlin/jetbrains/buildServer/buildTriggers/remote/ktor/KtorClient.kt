package jetbrains.buildServer.buildTriggers.remote.ktor

import com.intellij.openapi.diagnostic.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.ClientEngineClosedException
import io.ktor.client.features.HttpRequestTimeoutException
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.ServerResponseException
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.accept
import io.ktor.client.request.host
import io.ktor.client.request.port
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.network.sockets.ConnectTimeoutException
import jetbrains.buildServer.buildTriggers.remote.Constants
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val connectTimeout = 5L
private const val requestTimeout = 10L
private val timeUnit = TimeUnit.SECONDS

internal class KtorClient(private val myHost: String, private val myPort: Int) {
    private val myLogger = Logger.getInstance(KtorClient::class.qualifiedName)
    private val client = initClient()
    var outdated = false
        private set

    fun sendTriggerBuild(context: Map<String, String>): Boolean? = runBlocking {
        try {
            val response = client.post<Map<String, String>> { body = context }

            if (response.containsKey(Constants.Response.ERROR))
                myLogger.warn("Server responded with an error: ${response[Constants.Response.ERROR]}")

            response[Constants.Response.ANSWER]?.toBoolean() ?: run {
                myLogger.warn("Server response does not contain the ${Constants.Response.ANSWER} field")
                null
            }
        } catch (e: Exception) {
            when (e) {
                is ConnectTimeoutException ->
                    myLogger.error("Connection timed out to $myHost:$myPort")
                is HttpRequestTimeoutException ->
                    myLogger.error("Request timed out to $myHost:$myPort")
                is ServerResponseException -> // status code 5**
                    myLogger.error("Server $myHost:$myPort failed to respond", e)
                is ClientEngineClosedException ->
                    myLogger.error("Tried to use already closed connection to $myHost:$myPort")
                is IOException -> {
                    myLogger.error("Connection closed to $myHost:$myPort", e)
                    client.close()
                    outdated = true
                }
            }
            null
        }
    }

    fun closeConnection() {
        if (outdated) return

        client.close()
        outdated = true
    }

    private fun initClient() = HttpClient {
        defaultRequest {
            host = myHost
            port = myPort
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
        install(HttpTimeout) {
            connectTimeoutMillis = timeUnit.toMillis(connectTimeout)
            requestTimeoutMillis = timeUnit.toMillis(requestTimeout)
        }
    }
}