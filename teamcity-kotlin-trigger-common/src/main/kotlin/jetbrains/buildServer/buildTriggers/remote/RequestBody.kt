package jetbrains.buildServer.buildTriggers.remote

sealed class RequestBody

class TriggerContext(
    val currentTime: Long,
    val properties: Map<String, String>,
    val customData: MutableMap<String, String>,
    val buildType: BuildType
)

class TriggerBuildRequestBody(
    val context: TriggerContext,
    val authToken: String?
): RequestBody()

class TriggerPolicyBody(val bytes: ByteArray): RequestBody()
