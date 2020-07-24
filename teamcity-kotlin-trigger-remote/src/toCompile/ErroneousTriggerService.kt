package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*

class ErroneousTriggerService : TriggerService {
    override fun triggerBuild(context: TriggerContext): Boolean {
        throw RuntimeException("I'm the Evil")
    }
}