package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*

class DemandTrigger : Trigger {
    override fun triggerBuild(request: TriggerBuildRequest) = request.enable
}