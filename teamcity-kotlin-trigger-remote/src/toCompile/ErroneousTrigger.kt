package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*

class ErroneousTrigger : Trigger {
    override fun triggerBuild(request: TriggerBuildRequest): Boolean {
        throw RuntimeException("I'm the Evil")
    }
}