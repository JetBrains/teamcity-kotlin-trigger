package jetbrains.buildServer.buildTriggers.remote

interface CustomTriggerPolicy {
    fun PolicyContext.triggerBuild(context: TriggerContext): Boolean
}

interface PolicyContext {
    suspend fun get(url: String, responseFormat: Format = Format.Json): String
}

enum class Format(val contentType: String) {
    PlainText("text/plain"),
    Json("application/json"),
    Xml("application/xml")
}
