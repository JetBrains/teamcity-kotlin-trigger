package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.CustomTriggerPolicy
import jetbrains.buildServer.buildTriggers.remote.PolicyContext
import jetbrains.buildServer.buildTriggers.remote.TriggerContext
import kotlinx.coroutines.runBlocking

class SimpleTriggerWithRest : CustomTriggerPolicy {
    override fun PolicyContext.triggerBuild(context: TriggerContext): Boolean {
        val response = runBlocking { get("projects") }
        throw RuntimeException(response)
    }
}