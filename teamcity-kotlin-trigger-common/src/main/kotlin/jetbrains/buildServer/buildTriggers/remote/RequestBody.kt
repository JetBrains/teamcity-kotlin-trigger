package jetbrains.buildServer.buildTriggers.remote

sealed class RequestBody

class TriggerBuildRequestBody(
    val context: TriggerContext,
    val authToken: String?
): RequestBody()

class TriggerPolicyBody(val bytes: ByteArray): RequestBody()
