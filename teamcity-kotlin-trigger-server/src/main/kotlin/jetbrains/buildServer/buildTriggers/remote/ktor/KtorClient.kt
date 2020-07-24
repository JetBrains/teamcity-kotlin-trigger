package jetbrains.buildServer.buildTriggers.remote.ktor

import com.intellij.openapi.diagnostic.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.ClientEngineClosedException
import io.ktor.client.features.HttpRequestTimeoutException
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.ServerResponseException
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.accept
import io.ktor.client.request.host
import io.ktor.client.request.port
import io.ktor.client.request.request
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.network.sockets.ConnectTimeoutException
import jetbrains.buildServer.buildTriggers.remote.*
import jetbrains.buildServer.buildTriggers.remote.jackson.TypeValidator
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val connectTimeout = 5L
private const val requestTimeout = 10L
private val timeUnit = TimeUnit.SECONDS

internal class KtorClient(private val myHost: String, private val myPort: Int) {
    private val myLogger = Logger.getInstance(KtorClient::class.qualifiedName)
    private val myClient = initClient()
    var outdated = false
        private set

    fun sendTriggerBuild(triggerName: String, context: TriggerContext): TriggerBuildResponse? =
        makeRequest(
            RequestMapping.triggerBuild(triggerName),
            context,
            onConnectionError = null
        ) { response: Response ->
            when (response) {
                is TriggerBuildResponse -> return response
                is ErroneousResponse -> throw response.error
                else -> myLogger.error("Server response type is invalid: $response")
            }
            null
        }

    fun uploadTrigger(triggerName: String, body: TriggerBody): Unit =
        makeRequest(
            RequestMapping.uploadTrigger(triggerName),
            body,
            onConnectionError = Unit
        ) { response: Response ->
            when (response) {
                is OkayResponse -> return
                is ErroneousResponse -> throw response.error
                else -> throw RuntimeException("Server response type is invalid: ${response::class.qualifiedName}")
            }
        }

    fun closeConnection() {
        if (outdated) return

        myClient.close()
        outdated = true
    }

    private inline fun <T, Req : RequestBody, reified Resp : Response> makeRequest(
        mapping: Mapping,
        body: Req,
        onConnectionError: T,
        onSuccess: (Resp) -> T
    ): T = try {
        val response = runBlocking {
            myClient.request<Resp>(mapping.path) {
                method = mapping.httpMethod
                this.body = body
            }
        }
        onSuccess(response)
    } catch (e: Throwable) {
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
                closeConnection()
            }
            else -> throw e
        }
        onConnectionError
    }

    private fun initClient() = HttpClient {
        defaultRequest {
            host = myHost
            port = myPort
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        install(JsonFeature) {
            serializer = JacksonSerializer {
                activateDefaultTyping(TypeValidator.myTypeValidator)
            }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = timeUnit.toMillis(connectTimeout)
            requestTimeoutMillis = timeUnit.toMillis(requestTimeout)
        }
    }
}
