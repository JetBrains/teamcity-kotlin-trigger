package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*

class ScheduleTriggerPolicy : CustomTriggerPolicy {
    override fun triggerBuild(context: TriggerContext): Boolean = with(context) {
        val delay = properties["delay"]?.toIntOrNull() ?: -1
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