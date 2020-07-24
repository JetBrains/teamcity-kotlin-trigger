package jetbrains.buildServer.buildTriggers.remote

import io.ktor.http.HttpMethod

// It is assumed that the trigger name is unique
object RequestMapping {
//    fun triggerActivated(triggerName: String) = Mapping("/triggerActivated/$triggerName", HttpMethod.Post)
//    fun triggerDeactivated(triggerName: String) = Mapping("/triggerDeactivated/$triggerName", HttpMethod.Post)
    fun triggerBuild(triggerName: String) = Mapping("/triggerBuild/$triggerName", HttpMethod.Post)
    fun uploadTrigger(triggerName: String) = Mapping("/trigger/$triggerName", HttpMethod.Put)
}

data class Mapping(val path: String, val httpMethod: HttpMethod)