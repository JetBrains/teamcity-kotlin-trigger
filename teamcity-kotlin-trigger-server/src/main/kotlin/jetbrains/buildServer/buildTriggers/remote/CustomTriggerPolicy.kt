

package jetbrains.buildServer.buildTriggers.remote

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.buildTriggers.async.BaseAsyncPolledBuildTrigger
import jetbrains.buildServer.buildTriggers.remote.ktor.KtorClient
import jetbrains.buildServer.buildTriggers.remote.net.ServerError
import jetbrains.buildServer.buildTriggers.remote.net.TriggerBuildRequestBody
import jetbrains.buildServer.buildTriggers.remote.net.TriggerPolicyBody
import jetbrains.buildServer.buildTriggers.remote.net.TriggerPolicyDoesNotExistError
import jetbrains.buildServer.util.TimeService
import java.io.File

private const val HOST = "localhost"
private const val PORT = 8080

class RemoteTriggerPolicy(
    private val myTimeService: TimeService,
    private val myCustomTriggersManager: CustomTriggersManager
) : BaseAsyncPolledBuildTrigger() {

    private val myLogger = Logger.getInstance(RemoteTriggerPolicy::class.qualifiedName)
    private lateinit var myKtorClient: KtorClient

    override fun triggerActivated(context: PolledTriggerContext) {
        createClientIfNeeded {
            myLogger.debug("Trigger activation initialized a new connection")
        }
    }

    override fun triggerDeactivated(context: PolledTriggerContext) = synchronized(this) {
        if (this::myKtorClient.isInitialized)
            myKtorClient.closeConnection()
    }

    override fun triggerBuild(prev: String?, context: PolledTriggerContext): String? {
        val policyName = TriggerUtil.getTargetTriggerPolicyName(context.triggerDescriptor.properties) ?: run {
            myLogger.debug("Trigger policy not specified, triggerBuild() invocation skipped")
            return null
        }
        val policyDescriptor = CustomTriggerPolicyDescriptor(policyName, context.buildType.project)
        val policyPath = myCustomTriggersManager.getTriggerPolicyFilePath(policyDescriptor) ?: run {
            throw RuntimeException("Policy does not exist")
        }

        if (!myCustomTriggersManager.isTriggerPolicyEnabled(policyDescriptor))
            return null

        if (myCustomTriggersManager.isTriggerPolicyUpdated(policyDescriptor)) {
            myLogger.debug("Trigger policy '$policyName' was updated and will be uploaded")
            uploadTriggerPolicy(policyPath, policyName) {
                myCustomTriggersManager.setTriggerPolicyUpdated(policyDescriptor, false)
                doTriggerBuild(policyPath, policyName, context, true)
            }
        } else {
            doTriggerBuild(policyPath, policyName, context, true)
        }

        return null
    }

    private fun doTriggerBuild(
        policyPath: String,
        policyName: String,
        context: PolledTriggerContext,
        shouldTryUpload: Boolean = true
    ) {
        val client = createClientIfNeeded {
            myLogger.debug("TriggerBuild action initialized a new connection")
        }

        val policyDescriptor = CustomTriggerPolicyDescriptor(policyName, context.buildType.project)
        val triggerBuildContext = TriggerUtil.createTriggerBuildContext(context, myTimeService)
        val authToken = myCustomTriggersManager.getTriggerPolicyAuthToken(policyDescriptor)

        val triggerBuildRequestBody = TriggerBuildRequestBody(triggerBuildContext, authToken)

        val triggerBuildResponse = try {
            client.sendTriggerBuild(policyName, triggerBuildRequestBody)
        } catch (e: TriggerPolicyDoesNotExistError) {
            myLogger.warn(e.message)
            if (shouldTryUpload) {
                myLogger.debug("TriggerBuild action failed and invoked UploadTrigger")
                uploadTriggerPolicy(policyPath, policyName) {
                    myLogger.debug("UploadTrigger action succeeded and invoked TriggerBuild")
                    doTriggerBuild(policyPath, policyName, context, false)
                }
            } else myLogger.error("Trigger policy '$policyName' won't be uploaded, TriggerBuild invocation skipped")

            return
        } catch (se: ServerError) {
            myLogger.error("Failed to call TriggerBuild on trigger policy '$policyName', server responded with an error: $se")
            return
        } catch (e: Throwable) {
            myLogger.error("Failed to call TriggerBuild on trigger policy '$policyName'", e)
            return
        }

        TriggerUtil.getCustomDataStorageOfTrigger(context)
            .putValues(triggerBuildResponse.customData)

        if (triggerBuildResponse.answer)
            context.buildType.addToQueue(policyName)
    }

    private fun uploadTriggerPolicy(policyPath: String, policyName: String, onSuccess: () -> Unit = {}) {
        val client = createClientIfNeeded {
            myLogger.debug("UploadTrigger action initialized a new connection")
        }

        val triggerPolicyBytes = File(policyPath).readBytes()
        if (triggerPolicyBytes.isEmpty()) {
            myLogger.error("Failed to upload trigger policy '$policyName': it's file is absent")
            return
        }

        try {
            client.uploadTriggerPolicy(policyName, TriggerPolicyBody(triggerPolicyBytes))
        } catch (se: ServerError) {
            myLogger.error("Failed to upload trigger policy '$policyName', server responded with an error: $se")
            return
        } catch (e: Throwable) {
            myLogger.error("Failed to upload trigger policy '$policyName' due to an exception", e)
            return
        }
        onSuccess()
    }

    private fun createClientIfNeeded(onCreate: () -> Unit = {}): KtorClient = synchronized(this) {
        myKtorClient =
            if (!this::myKtorClient.isInitialized || myKtorClient.closed) {
                onCreate()
                KtorClient(HOST, PORT)
            } else myKtorClient

        myKtorClient
    }
}