package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*

class DemandTriggerPolicy : CustomTriggerPolicy {
    override fun triggerBuild(context: TriggerContext) = context.properties["enable"]?.toBoolean() ?: false
}