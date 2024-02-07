

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