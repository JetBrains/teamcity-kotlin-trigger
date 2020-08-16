package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*

class HangingTriggerPolicy : CustomTriggerPolicy {
    override fun PolicyContext.triggerBuild(context: TriggerContext): Boolean {
        Thread.sleep(10_000)
        return true
    }
}