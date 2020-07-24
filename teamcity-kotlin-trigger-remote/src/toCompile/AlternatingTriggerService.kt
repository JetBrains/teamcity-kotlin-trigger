package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*

class AlternatingTriggerService : TriggerService {
    override fun triggerBuild(context: TriggerContext): Boolean {
        val shouldTrigger = context.customData["shouldTrigger"]?.toBoolean() ?: true
        context.customData["shouldTrigger"] = (!shouldTrigger).toString()
        return shouldTrigger
    }
}