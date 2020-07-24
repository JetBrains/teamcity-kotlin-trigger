package jetbrains.buildServer.buildTriggers.remote

interface CustomTriggerPolicy {
    fun triggerBuild(context: TriggerContext): Boolean
}
