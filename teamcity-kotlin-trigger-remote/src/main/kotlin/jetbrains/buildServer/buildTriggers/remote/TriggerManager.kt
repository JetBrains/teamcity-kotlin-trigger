package jetbrains.buildServer.buildTriggers.remote

internal interface TriggerManager {
    fun loadTrigger(triggerName: String): TriggerService
    fun saveTrigger(triggerName: String, bytes: ByteArray)

}

class TriggerDoesNotExistException : RuntimeException()
