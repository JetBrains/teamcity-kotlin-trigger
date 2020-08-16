package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*

class AlternatingTriggerPolicy : CustomTriggerPolicy {
    override fun PolicyContext.triggerBuild(context: TriggerContext): Boolean {
        val shouldTrigger = context.customData["shouldTrigger"]?.toBoolean() ?: true
        context.customData["shouldTrigger"] = (!shouldTrigger).toString()
        return shouldTrigger
    }
}