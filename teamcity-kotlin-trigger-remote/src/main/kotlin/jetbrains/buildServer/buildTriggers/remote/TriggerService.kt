package jetbrains.buildServer.buildTriggers.remote

interface TriggerService {
    fun triggerBuild(context: TriggerContext): Boolean
}
