package jetbrains.buildServer.buildTriggers.remote

internal interface Trigger {
    fun triggerBuild(context: Map<String, String>): Boolean
}
