package jetbrains.buildServer.buildTriggers.remote.ktor

import jetbrains.buildServer.buildTriggers.remote.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.logging.Logger

private const val TIMEOUT = 5L
private val TIMEOUT_UNIT = TimeUnit.SECONDS

internal class TriggerActions(
    private val myTriggerPolicyManager: TriggerPolicyManager,
    private val myLogger: Logger
) {

    fun triggerBuild(triggerPolicyName: String, requestBody: TriggerBuildRequestBody): TriggerBuildResponse {
        var executorThread: Thread? = null
        val executor = Executors.newSingleThreadExecutor()

        val policyContext = PolicyContextImpl(requestBody.authToken)

        val future = executor.submit<Boolean> {
            executorThread = Thread.currentThread()

            loadTriggerPolicy(triggerPolicyName).run {
                policyContext.triggerBuild(requestBody.context)
            }
        }

        val answer = try {
            future.get(TIMEOUT, TIMEOUT_UNIT)
        } catch (e: TimeoutException) {
            executorThread!!.stop()
            executor.shutdownNow()
            throw triggerInvocationTimeoutError(triggerPolicyName)

        } catch (e: ExecutionException) {
            executor.shutdownNow()
            myLogger.severe("Failed to call triggerBuild() on trigger policy $triggerPolicyName")

            if (e.cause is TriggerPolicyDoesNotExistError || e.cause is TriggerPolicyLoadingError)
                throw e.cause!!

            throw internalTriggerPolicyError(e.cause!!)
        } finally {
            policyContext.close()
        }

        myLogger.info("Trigger $triggerPolicyName is sending response: $answer")
        return TriggerBuildResponse(answer, requestBody.context.customData)
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
            e.printStackTrace()
            throw triggerPolicyLoadingError(e)
        }
}
