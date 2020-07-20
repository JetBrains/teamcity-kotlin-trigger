package jetbrains.buildServer.buildTriggers.remote

sealed class Request

class TriggerBuildRequest(
    val enable: Boolean,
    val delay: Int,
    val previousCallTime: Long?,
    val currentTime: Long
): Request()

class UploadTriggerRequest(val triggerBody: ByteArray): Request()
