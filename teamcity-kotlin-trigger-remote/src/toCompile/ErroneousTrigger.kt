package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*

class ErroneousTrigger : Trigger {
    override fun triggerBuild(context: Map<String, String>): Boolean {
        throw RuntimeException("I'm the Evil")
    }
}