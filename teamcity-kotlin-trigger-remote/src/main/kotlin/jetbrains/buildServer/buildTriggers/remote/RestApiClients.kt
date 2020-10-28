/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
