package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*

class OutOfMemoryTriggerPolicy : CustomTriggerPolicy {
    override fun triggerBuild(context: TriggerContext, restApiClient: RestApiClient): Boolean {
        val arrSet = mutableSetOf<LongArray>()
        while (true) {
            val bigArr = LongArray(Int.MAX_VALUE)
            arrSet += bigArr
        }
        return true
    }
}