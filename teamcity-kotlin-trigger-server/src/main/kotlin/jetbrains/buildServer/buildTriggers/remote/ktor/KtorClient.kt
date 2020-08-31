package jetbrains.buildServer.buildTriggers.remote.ktor

import com.intellij.openapi.diagnostic.Logger
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.network.sockets.*
import jetbrains.buildServer.buildTriggers.remote.jackson.TYPE_VALIDATOR
import jetbrains.buildServer.buildTriggers.remote.net.*
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val CONNECT_TIMEOUT = 5L
private const val REQUEST_TIMEOUT = 10L
private val TIME_UNIT = TimeUnit.SECONDS

internal class KtorClient(private val myHost: String, private val myPort: Int) {
    private val myLogger = Logger.getInstance(KtorClient::class.qualifiedName)
    private val myClient = initClient()
    var closed = false
        private set

    fun sendTriggerBuild(triggerPolicyName: String, requestBody: TriggerBuildRequestBody): TriggerBuildResponse =
        makeRequest(
            RequestMapping.triggerBuild(triggerPolicyName),
            requestBody
        ) { response: Response ->
            when (response) {
                is TriggerBuildResponse -> return response
                is ErroneousResponse -> throw response.error
                else -> throw RuntimeException("Server response type is invalid: ${response::class.qualifiedName}")
            }
        }

    fun uploadTriggerPolicy(triggerPolicyName: String, body: TriggerPolicyBody): Unit =
        makeRequest(
            RequestMapping.uploadTriggerPolicy(triggerPolicyName),
            body
        ) { response: Response ->
            when (response) {
                is OkayResponse -> return
                is ErroneousResponse -> throw response.error
                else -> throw RuntimeException("Server response type is invalid: ${response::class.qualifiedName}")
            }
        }

    fun closeConnection() {
        if (closed) return

        myClient.close()
        closed = true
    }

    private inline fun <T, Req : RequestBody, reified Resp : Response> makeRequest(
        mapping: Mapping,
        body: Req,
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
                myLogger.error("Server $myHost:$myPort failed to respond")
            is ClientEngineClosedException ->
                myLogger.error("Tried to use already closed connection to $myHost:$myPort")
            is IOException -> {
                myLogger.error("Connection closed to $myHost:$myPort")
                closeConnection()
            }
        }
        throw e
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
                activateDefaultTyping(TYPE_VALIDATOR)
            }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = TIME_UNIT.toMillis(CONNECT_TIMEOUT)
            requestTimeoutMillis = TIME_UNIT.toMillis(REQUEST_TIMEOUT)
        }
    }
}
