package jetbrains.buildServer.buildTriggers.remote

sealed class RequestBody

class TriggerContext(
    val currentTime: Long,
    val properties: Map<String, String>,
    val customData: MutableMap<String, String>
): RequestBody()

class TriggerBody(val bytes: ByteArray): RequestBody()
