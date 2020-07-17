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
import io.ktor.client.request.request
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.network.sockets.ConnectTimeoutException
import jetbrains.buildServer.buildTriggers.remote.Constants
import jetbrains.buildServer.buildTriggers.remote.Mapping
import jetbrains.buildServer.buildTriggers.remote.RequestMapping
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

    suspend fun sendTriggerBuild(triggerName: String, context: Map<String, String>): Boolean? =
        makeRequest(
            RequestMapping.triggerBuild(triggerName),
            context,
            @Suppress("USELESS_CAST") { null as Boolean? } // without this cast makeRequest will infer Nothing? type
        ) { response: Map<String, String> ->
            if (response.containsKey(Constants.Response.ERROR))
                myLogger.warn("Server responded with an error: ${response[Constants.Response.ERROR]}")

            response[Constants.Response.ANSWER]?.toBoolean() ?: run {
                myLogger.warn("Server response does not contain the ${Constants.Response.ANSWER} field")
                null
            }
        }

    suspend fun uploadTrigger(triggerName: String, trigger: ByteArray): Boolean =
        makeRequest(
            RequestMapping.uploadTrigger(triggerName),
            trigger,
            { false }
        ) { response: Map<String, String> ->
            if (response.containsKey(Constants.Response.ERROR))
                myLogger.warn("Server responded with an error: ${response[Constants.Response.ERROR]}")

            response[Constants.Response.ANSWER]?.toBoolean() ?: run {
                myLogger.warn("Server response does not contain the ${Constants.Response.ANSWER} field")
                false
            }
        }

    fun closeConnection() {
        if (outdated) return

        client.close()
        outdated = true
    }

    private suspend inline fun <T, reified E> makeRequest(
        mapping: Mapping,
        body: Any,
        onError: (Throwable) -> T,
        onSuccess: (E) -> T
    ): T = try {
        val response = client.request<E>(mapping.path) {
            method = mapping.httpMethod
            this.body = body
        }
        onSuccess(response)
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
        onError(e)
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
