package jetbrains.buildServer.buildTriggers.remote.netty

internal sealed class Event(val myParams: Map<String, String>)

internal class TriggerBuild(params: Map<String, String>) : Event(params)