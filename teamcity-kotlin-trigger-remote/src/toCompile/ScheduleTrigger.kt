package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*
import jetbrains.buildServer.buildTriggers.remote.Constants.Request.ENABLE
import jetbrains.buildServer.buildTriggers.remote.Constants.Request.DELAY
import jetbrains.buildServer.buildTriggers.remote.Constants.Request.CURRENT_TIME
import jetbrains.buildServer.buildTriggers.remote.Constants.Request.PREVIOUS_CALL_TIME

class ScheduleTrigger : Trigger {
    override fun triggerBuild(context: Map<String, String>): Boolean {
        val enableStr = context[ENABLE] ?: return false
        val delayStr = context[DELAY] ?: return false
        val prevCallTimeStr = context[PREVIOUS_CALL_TIME]
        val currTimeStr = context[CURRENT_TIME] ?: return false

        val enable = enableStr.toBoolean()
        val delay = delayStr.toLongOrNull()
        val prevCallTime = prevCallTimeStr?.toLongOrNull()
        val currTime = currTimeStr.toLong()

        return if (!enable || null == delay) false
        else prevCallTime == null || currTime - prevCallTime > delay * 60_000
    }
}