package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*

class OutOfMemoryTriggerPolicy : CustomTriggerPolicy {
    override fun triggerBuild(context: TriggerContext): Boolean {
        val arrSet = mutableSetOf<LongArray>()
        while (true) {
            val bigArr = LongArray(Int.MAX_VALUE)
            bigArr[0] = 1L
            arrSet += bigArr
        }
        return true
    }
}