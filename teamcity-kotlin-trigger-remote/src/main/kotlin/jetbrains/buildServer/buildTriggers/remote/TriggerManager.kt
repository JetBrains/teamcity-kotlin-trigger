package jetbrains.buildServer.buildTriggers.remote

internal interface TriggerManager {
    fun loadTrigger(triggerName: String): Trigger
    fun saveTrigger(triggerName: String, bytes: ByteArray)

}

class TriggerDoesNotExistException : RuntimeException()
