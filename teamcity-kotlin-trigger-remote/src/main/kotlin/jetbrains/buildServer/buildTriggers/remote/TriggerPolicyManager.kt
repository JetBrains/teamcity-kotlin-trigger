package jetbrains.buildServer.buildTriggers.remote

internal interface TriggerPolicyManager {
    fun loadTriggerPolicy(triggerPolicyName: String): CustomTriggerPolicy
    fun saveTriggerPolicy(triggerPolicyName: String, bytes: ByteArray)
}

internal class TriggerPolicyDoesNotExistException : RuntimeException()
