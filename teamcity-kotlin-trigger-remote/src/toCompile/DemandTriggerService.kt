package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*

class DemandTriggerService : TriggerService {
    override fun triggerBuild(context: TriggerContext) = context.properties["enable"]?.toBoolean() ?: false
}