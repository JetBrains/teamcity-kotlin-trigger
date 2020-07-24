package jetbrains.buildServer.buildTriggers.remote

import io.ktor.http.HttpMethod

object RequestMapping {
    fun triggerBuild(triggerPolicyName: String) = Mapping("/triggerBuild/$triggerPolicyName", HttpMethod.Post)
    fun uploadTriggerPolicy(triggerPolicyName: String) = Mapping("/triggerPolicy/$triggerPolicyName", HttpMethod.Put)
}

data class Mapping(val path: String, val httpMethod: HttpMethod)