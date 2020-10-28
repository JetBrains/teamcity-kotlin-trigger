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

interface CustomTriggerPolicy {
    fun triggerBuild(context: TriggerContext, restApiClient: RestApiClient): Boolean
}

interface RestApiClient {
    suspend fun get(path: String, responseFormat: Format = Format.Json): String
}

enum class Format(val contentType: String) {
    PlainText("text/plain"),
    Json("application/json"),
    Xml("application/xml")
}
