package jetbrains.buildServer.buildTriggers.remote

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.buildTriggers.async.BaseAsyncPolledBuildTrigger
import jetbrains.buildServer.buildTriggers.remote.ktor.KtorClient
import jetbrains.buildServer.util.TimeService
import java.io.File

private const val HOST = "localhost"
private const val PORT = 8080

class RemoteTriggerPolicy(
    private val myTimeService: TimeService,
    private val myCustomTriggersBean: CustomTriggersManager
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
        val triggerPolicyPath = TriggerUtil.getTargetTriggerPolicyPath(context.triggerDescriptor.properties)
            ?: run {
                myLogger.debug("Trigger policy not specified, triggerBuild() invocation skipped")
                return null
            }
        if (!myCustomTriggersBean.isTriggerPolicyEnabled(triggerPolicyPath))
            return null

        if (myCustomTriggersBean.isTriggerPolicyUpdated(triggerPolicyPath)) {
            val triggerPolicyName = CustomTriggerPolicyDescriptor.policyPathToPolicyName(triggerPolicyPath)
            myLogger.debug("Trigger policy '$triggerPolicyName' was updated and will be uploaded")
            uploadTriggerPolicy(triggerPolicyPath) {
                myCustomTriggersBean.setTriggerPolicyUpdated(triggerPolicyPath, false)
                doTriggerBuild(triggerPolicyPath, context, true)
            }
        } else {
            doTriggerBuild(triggerPolicyPath, context, true)
        }

        return null
    }

    private fun doTriggerBuild(
        triggerPolicyPath: String,
        context: PolledTriggerContext,
        shouldTryUpload: Boolean = true
    ) {
        val client = createClientIfNeeded {
            myLogger.debug("TriggerBuild action initialized a new connection")
        }

        val triggerPolicyName = CustomTriggerPolicyDescriptor.policyPathToPolicyName(triggerPolicyPath)
        val triggerBuildContext = TriggerUtil.createTriggerBuildContext(context, myTimeService)

        val triggerBuildResponse = try {
            client.sendTriggerBuild(triggerPolicyName, triggerBuildContext)
        } catch (e: TriggerPolicyDoesNotExistError) {
            myLogger.warn(e.message)
            if (shouldTryUpload) {
                myLogger.debug("TriggerBuild action failed and invoked UploadTrigger")
                uploadTriggerPolicy(triggerPolicyPath) {
                    myLogger.debug("UploadTrigger action succeeded and invoked TriggerBuild")
                    doTriggerBuild(triggerPolicyPath, context, false)
                }
            } else myLogger.error("Trigger policy '$triggerPolicyName' won't be uploaded, TriggerBuild invocation skipped")

            return
        } catch (se: ServerError) {
            myLogger.error("Failed to call TriggerBuild on trigger policy '$triggerPolicyName', server responded with an error: $se")
            return
        } catch (e: Throwable) {
            myLogger.error("Failed to call TriggerBuild on trigger policy '$triggerPolicyName'", e)
            return
        }

        TriggerUtil.getCustomDataStorageOfTrigger(context)
            .putValues(triggerBuildResponse.customData)

        if (triggerBuildResponse.answer)
            context.buildType.addToQueue(triggerPolicyName)
    }

    private fun uploadTriggerPolicy(triggerPolicyPath: String, onSuccess: () -> Unit = {}) {
        val client = createClientIfNeeded {
            myLogger.debug("UploadTrigger action initialized a new connection")
        }

        val triggerPolicyName = CustomTriggerPolicyDescriptor.policyPathToPolicyName(triggerPolicyPath)
        val triggerPolicyBytes = File(triggerPolicyPath).readBytes()

        try {
            client.uploadTriggerPolicy(triggerPolicyName, TriggerPolicyBody(triggerPolicyBytes))
        } catch (se: ServerError) {
            myLogger.error("Failed to upload trigger policy '$triggerPolicyName', server responded with an error: $se")
            return
        } catch (e: Throwable) {
            myLogger.error("Failed to upload trigger policy '$triggerPolicyName' due to an exception", e)
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
