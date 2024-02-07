

package jetbrains.buildServer.buildTriggers.remote.net

import jetbrains.buildServer.buildTriggers.remote.TriggerContext

sealed class RequestBody

class TriggerBuildRequestBody(
    val context: TriggerContext,
    val authToken: String?
): RequestBody()

class TriggerPolicyBody(val bytes: ByteArray): RequestBody()