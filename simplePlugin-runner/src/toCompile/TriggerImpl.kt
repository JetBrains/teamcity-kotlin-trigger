package com.jetbrains.teamcity.boris.simplePlugin.compiled

import com.jetbrains.teamcity.boris.simplePlugin.*

class TriggerImpl: Trigger {
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