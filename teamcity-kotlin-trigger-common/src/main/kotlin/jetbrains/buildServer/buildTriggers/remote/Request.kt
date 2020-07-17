package jetbrains.buildServer.buildTriggers.remote

import io.ktor.http.HttpMethod

data class Request(val context: Map<String, String>)

object RequestMapping {
    fun triggerBuild(triggerName: String) = Mapping("/trigger/$triggerName", HttpMethod.Post)
    fun uploadTrigger(triggerName: String) = Mapping("/trigger/$triggerName", HttpMethod.Put)
}

data class Mapping(val path: String, val httpMethod: HttpMethod)
