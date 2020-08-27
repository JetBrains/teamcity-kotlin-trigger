package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.CustomTriggerPolicy
import jetbrains.buildServer.buildTriggers.remote.RestApiClient
import jetbrains.buildServer.buildTriggers.remote.TriggerContext
import kotlinx.coroutines.runBlocking

class SimpleTriggerWithRest : CustomTriggerPolicy {
    override fun triggerBuild(context: TriggerContext, restApiClient: RestApiClient): Boolean {
        val response = runBlocking { restApiClient.get("projects") }
        throw RuntimeException(response)
    }
}