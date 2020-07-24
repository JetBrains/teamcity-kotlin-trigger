package jetbrains.buildServer.buildTriggers.remote

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.buildTriggers.async.BaseAsyncPolledBuildTrigger
import jetbrains.buildServer.buildTriggers.remote.ktor.KtorClient
import jetbrains.buildServer.util.TimeService
import java.io.File

private const val host = "localhost"
private const val port = 8080

class RemoteTriggerPolicy(myTimeService: TimeService) :
    BaseAsyncPolledBuildTrigger() {

    private val myLogger = Logger.getInstance(RemoteTriggerPolicy::class.qualifiedName)
    private val myTriggerUtil = TriggerUtil(myTimeService)
    private lateinit var myKtorClient: KtorClient

    override fun triggerActivated(context: PolledTriggerContext) {
        createClientIfNeeded {
            myLogger.debug("Trigger activation initialized a new connection")
        }
    }

    override fun triggerDeactivated(context: PolledTriggerContext) = synchronized(this) {
        if (this::myKtorClient.isInitialized && !myKtorClient.outdated)
            myKtorClient.closeConnection()
    }

    override fun triggerBuild(prev: String?, context: PolledTriggerContext): String? {
        val triggerPath = TriggerUtil.getTargetTriggerPath(context.triggerDescriptor.properties) ?: run {
            myLogger.debug("Trigger not specified, triggerBuild() invocation skipped")
            return null
        }

        doTriggerBuild(triggerPath, context, true)
        return null
    }

    private fun uploadTrigger(triggerPath: String, onSuccess: () -> Unit = {}) {
        val triggerName = TriggerUtil.getTriggerName(triggerPath)

        val client = createClientIfNeeded {
            myLogger.debug("UploadTrigger action initialized a new connection")
        }
        val triggerBytes = File(triggerPath).readBytes()
        try {
            client.uploadTrigger(triggerName, TriggerBody(triggerBytes))
        } catch (se: ServerError) {
            myLogger.error("Failed to upload trigger `$triggerName`, server responded with an error: $se")
            return
        } catch (e: Throwable) {
            myLogger.error("Failed to upload trigger `$triggerName` due to an unknown exception", e)
            return
        }
        onSuccess()
    }

    private fun doTriggerBuild(triggerPath: String, context: PolledTriggerContext, shouldTryUpload: Boolean = true) {
        val triggerName = TriggerUtil.getTriggerName(triggerPath)
        val triggerBuildContext = myTriggerUtil.createTriggerBuildContext(context)
        val client = createClientIfNeeded {
            myLogger.debug("TriggerBuild action initialized a new connection")
        }

        val triggerBuildResponse = try {
            client.sendTriggerBuild(triggerName, triggerBuildContext)
        } catch (e: TriggerDoesNotExistError) {
            myLogger.warn(e.message ?: "Trigger '$triggerName' does not exist on the server")
            if (shouldTryUpload) {
                myLogger.debug("TriggerBuild action failed and invoked UploadTrigger")
                uploadTrigger(triggerPath) {
                    myLogger.debug("UploadTrigger action succeeded and invoked TriggerBuild")
                    doTriggerBuild(triggerPath, context, false)
                }
            } else myLogger.error("Trigger `$triggerName` won't be uploaded, TriggerBuild invocation skipped")

            return
        } catch (se: ServerError) {
            myLogger.error("Failed to call TriggerBuild on trigger `$triggerName`, server responded with an error: $se")
            return
        } catch (e: Throwable) {
            myLogger.error("Failed to call TriggerBuild on trigger `$triggerName` due to an unknown exception", e)
            return
        }

        if (triggerBuildResponse != null) {
            TriggerUtil.getCustomDataStorage(context)
                .putValues(triggerBuildResponse.customData)

            if (triggerBuildResponse.answer)
                context.buildType.addToQueue(triggerName)
        } else {
            myLogger.debug("Failed to call TriggerBuild on trigger '$triggerName'")
        }
    }

    private fun createClientIfNeeded(onCreate: () -> Unit = {}): KtorClient = synchronized(this) {
        myKtorClient =
            if (!this::myKtorClient.isInitialized || myKtorClient.outdated) {
                onCreate()
                KtorClient(host, port)
            } else myKtorClient

        myKtorClient
    }
}
