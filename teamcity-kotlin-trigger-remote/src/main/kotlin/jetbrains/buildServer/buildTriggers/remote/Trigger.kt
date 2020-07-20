package jetbrains.buildServer.buildTriggers.remote

internal interface Trigger {
    fun triggerBuild(request: TriggerBuildRequest): Boolean
}
