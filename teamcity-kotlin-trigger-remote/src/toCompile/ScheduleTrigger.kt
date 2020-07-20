package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*

class ScheduleTrigger : Trigger {
    override fun triggerBuild(request: TriggerBuildRequest): Boolean = with(request) {
        when {
            !enable -> false
            previousCallTime == null -> true
            else -> currentTime - previousCallTime > delay * 60_000
        }
    }
}