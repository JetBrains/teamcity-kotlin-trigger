package jetbrains.buildServer.buildTriggers.remote.ktor

import jetbrains.buildServer.buildTriggers.remote.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.logging.Logger

private const val timeout = 5L
private val timeoutUnit = TimeUnit.SECONDS

internal class TriggerActions(
    private val myTriggerPolicyManager: TriggerPolicyManager, private val myLogger: Logger
) {
    private val myExecutor = Executors.newSingleThreadExecutor()

    fun triggerBuild(triggerPolicyName: String, context: TriggerContext): TriggerBuildResponse {
        val triggerPolicy = loadTriggerPolicy(triggerPolicyName)
        val answer = try {
            myExecutor.submit<Boolean> {
                triggerPolicy.triggerBuild(context)
            }.get(timeout, timeoutUnit)
        } catch (e: TimeoutException) {
            throw triggerInvocationTimeoutError(triggerPolicyName)
        } catch (e: ExecutionException) {
            myLogger.severe("Failed to call triggerBuild() on trigger policy $triggerPolicyName")
            throw internalTriggerPolicyError(e.cause!!)
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
