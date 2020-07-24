package jetbrains.buildServer.buildTriggers.remote.ktor

import jetbrains.buildServer.buildTriggers.remote.*
import java.util.logging.Logger

internal class ActionHandler(
    private val myTriggerPolicyManager: TriggerPolicyManager, private val myLogger: Logger
) {
    fun triggerBuild(triggerPolicyName: String, context: TriggerContext): TriggerBuildResponse {
        val triggerPolicy = loadTriggerPolicy(triggerPolicyName)
        val answer = try {
            triggerPolicy.triggerBuild(context)
        } catch (e: Throwable) {
            myLogger.severe("Failed to call triggerBuild() on trigger policy $triggerPolicyName")
            throw internalTriggerPolicyError(e)
        }

        myLogger.info("Trigger $triggerPolicyName is sending response: $answer")
        return TriggerBuildResponse(answer, context.customData)
    }

    fun saveTriggerPolicy(triggerPolicyName: String, triggerPolicyBody: TriggerPolicyBody): OkayResponse {
        myTriggerPolicyManager.saveTriggerPolicy(triggerPolicyName, triggerPolicyBody.bytes)
        myLogger.info("Trigger policy $triggerPolicyName loaded")
        return OkayResponse
    }

    private fun loadTriggerPolicy(triggerPolicyName: String): CustomTriggerPolicy =
        try {
            myTriggerPolicyManager.loadTriggerPolicy(triggerPolicyName)
        } catch (e: TriggerPolicyDoesNotExistException) {
            throw triggerPolicyDoesNotExistError(triggerPolicyName)
        } catch (e: Throwable) {
            throw triggerPolicyLoadingError(e)
        }
}
