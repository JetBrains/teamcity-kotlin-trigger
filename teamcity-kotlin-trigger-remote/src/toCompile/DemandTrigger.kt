package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*
import jetbrains.buildServer.buildTriggers.remote.Constants.Request.ENABLE

class DemandTrigger : Trigger {
    override fun triggerBuild(context: Map<String, String>) = context[ENABLE]?.toBoolean() ?: false
}