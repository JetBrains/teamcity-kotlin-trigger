

package jetbrains.buildServer.buildTriggers.remote

internal interface TriggerPolicyManager {
    fun <T> loadTriggerPolicy(policyName: String, onLoad: (CustomTriggerPolicy) -> T): T
    fun saveTriggerPolicy(policyName: String, bytes: ByteArray)
}

internal class TriggerPolicyDoesNotExistException : RuntimeException()