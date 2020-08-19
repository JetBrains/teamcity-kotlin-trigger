package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*

class ErroneousTriggerPolicy : CustomTriggerPolicy {
    override fun triggerBuild(context: TriggerContext, restApiClient: RestApiClient): Boolean {
        throw RuntimeException("I'm the Evil")
    }
}