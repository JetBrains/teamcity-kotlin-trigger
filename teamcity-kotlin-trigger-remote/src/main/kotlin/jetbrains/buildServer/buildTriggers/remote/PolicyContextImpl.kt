package jetbrains.buildServer.buildTriggers.remote

import io.ktor.client.HttpClient
import io.ktor.client.features.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import java.io.Closeable

private const val HOST = "localhost"
private const val PORT = 8111
private const val BASE_URL = "bs/app/rest/"

class PolicyContextImpl(private val token: String? = null) : PolicyContext, Closeable {
    private val client = HttpClient {
        defaultRequest {
            host = HOST
            port = PORT
            if (token != null)
                header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    override suspend fun get(url: String, responseFormat: Format): String = client.get("$BASE_URL$url") {
        accept(ContentType.parse(responseFormat.contentType))
    }

    override fun close() = client.close()
}
