package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*
import jetbrains.buildServer.buildTriggers.remote.annotation.*

const val DELAY_PROPERTY = "delay"

@CustomTriggerProperty(DELAY_PROPERTY, PropertyType.TEXT, "Scheduling delay", true)
class ScheduleTriggerPolicy : CustomTriggerPolicy {
    override fun triggerBuild(context: TriggerContext, restApiClient: RestApiClient): Boolean = with(context) {
        val delay = properties[DELAY_PROPERTY]?.toIntOrNull() ?: -1
        val prevoiusSuccessTime = context.customData["previousSuccessTime"]?.toLongOrNull()
        val answer = when {
            delay <= 0 -> false
            prevoiusSuccessTime == null -> true
            else -> currentTime - prevoiusSuccessTime >= delay * 60_000
        }
        if (answer)
            context.customData["previousSuccessTime"] = currentTime.toString()
        answer
    }
}