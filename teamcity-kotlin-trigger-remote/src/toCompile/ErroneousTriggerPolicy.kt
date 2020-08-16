package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*

class ErroneousTriggerPolicy : CustomTriggerPolicy {
    override fun PolicyContext.triggerBuild(context: TriggerContext): Boolean {
        throw RuntimeException("I'm the Evil")
    }
}