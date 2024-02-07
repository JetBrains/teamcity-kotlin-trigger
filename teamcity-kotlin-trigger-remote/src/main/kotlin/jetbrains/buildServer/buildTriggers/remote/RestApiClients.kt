

package jetbrains.buildServer.buildTriggers.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.features.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import java.io.Closeable

private const val SERVER_URL = "http://localhost:8111/bs"
private const val REST_PATH = "app/rest"

sealed class CloseableKtorRestApiClient : RestApiClient, Closeable {
    protected val client = HttpClient { configureClient(this) }

    override fun close() {
        client.close()
    }

    protected open fun configureClient(config: HttpClientConfig<out HttpClientEngineConfig>) {}

    protected fun getServerPath(vararg pathComponents: String) =
        listOf(SERVER_URL, *pathComponents).joinToString(separator = "/") { it }
}

class AuthorizedRestApiClient(private val token: String) : CloseableKtorRestApiClient() {

    override suspend fun get(path: String, responseFormat: Format): String {
        val url = getServerPath(REST_PATH, path)
        return client.get(url) {
            accept(ContentType.parse(responseFormat.contentType))
        }
    }

    override fun configureClient(config: HttpClientConfig<out HttpClientEngineConfig>) = config.run {
        defaultRequest {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}

class GuestAuthRestApiClient : CloseableKtorRestApiClient() {

    override suspend fun get(path: String, responseFormat: Format): String {
        val url = getServerPath("guestAuth", REST_PATH, path)
        return client.get(url) {
            accept(ContentType.parse(responseFormat.contentType))
        }
    }
}