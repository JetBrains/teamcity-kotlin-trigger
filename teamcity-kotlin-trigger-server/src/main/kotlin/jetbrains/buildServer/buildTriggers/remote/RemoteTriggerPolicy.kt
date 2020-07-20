package jetbrains.buildServer.buildTriggers.remote

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.buildTriggers.async.AsyncPolledBuildTrigger
import jetbrains.buildServer.buildTriggers.remote.ktor.DeferredHelper
import jetbrains.buildServer.buildTriggers.remote.ktor.KtorClient
import jetbrains.buildServer.buildTriggers.remote.ktor.ValueNotCompleteException
import jetbrains.buildServer.util.TimeService
import java.io.File

private const val host = "localhost"
private const val port = 8080

private enum class State {
    UploadTrigger, TriggerBuild
}

class RemoteTriggerPolicy(myTimeService: TimeService) : AsyncPolledBuildTrigger {
    private val myLogger = Logger.getInstance(RemoteTriggerPolicy::class.qualifiedName)
    private val myTriggerUtil = TriggerUtil(myTimeService)
    private lateinit var myKtorClient: KtorClient
    private val myTriggerBuildResponseHelper = DeferredHelper<Boolean?>()
    private val myUploadTriggerHelper = DeferredHelper<Boolean>()

    private var myState: State = State.TriggerBuild

    override fun triggerActivated(context: PolledTriggerContext) = synchronized(this) {
        myKtorClient = getOrCreateClient {
            myLogger.debug("Trigger activation initialized a new connection")
        }
    }

    override fun triggerDeactivated(context: PolledTriggerContext): Unit = synchronized(this) {
        if (this::myKtorClient.isInitialized && !myKtorClient.outdated)
            myKtorClient.closeConnection()
    }

    override fun triggerBuild(prev: String?, context: PolledTriggerContext): String? = synchronized(this) {
        when (myState) {
            State.TriggerBuild -> triggerBuild(context)
            State.UploadTrigger -> uploadTrigger(context)
        }
        myState.name
    }

    private fun uploadTrigger(context: PolledTriggerContext) {
        val triggerPath = TriggerUtil.getTargetTriggerPath(context)
        val triggerName = TriggerUtil.getTargetTriggerName(context)

        val id = TriggerUtil.getTriggerId(context)
        try {
            myUploadTriggerHelper.tryComplete(id) {
                val client = getOrCreateClient {
                    myLogger.debug("triggerBuild() initialized a new connection")
                }
                val trigger = File(triggerPath).readBytes()
                val triggerUploaded = client.uploadTrigger(triggerName, UploadTriggerRequest(trigger))

                if (!triggerUploaded)
                    myLogger.error("Failed to upload trigger '$triggerName' to the server. Will retry")
                else myState = State.TriggerBuild

                triggerUploaded
            }
        } catch (e: ValueNotCompleteException) {
        } catch (e: ServerError) {
            myLogger.error("Server responded with an error: $e")
        }
    }

    private fun triggerBuild(context: PolledTriggerContext) {
        val triggerName = TriggerUtil.getTargetTriggerName(context)
        val id = TriggerUtil.getTriggerId(context)

        val response = try {
            myTriggerBuildResponseHelper.tryComplete(id) {
                val client = getOrCreateClient {
                    myLogger.debug("triggerBuild() initialized a new connection")
                }
                val contextSubset = myTriggerUtil.createTriggerBuildRequest(context)
                client.sendTriggerBuild(triggerName, contextSubset)
            }
        } catch (e: ValueNotCompleteException) {
            null
        } catch (e: TriggerDoesNotExistError) {
            myLogger.warn(e.message ?: "Trigger '$triggerName' does not exist")
            myState = State.UploadTrigger
            false
        } catch (e: ServerError) {
            myLogger.error("Server responded with an error: $e")
            null
        }

        when (response) {
            null -> {
                myLogger.debug("Failed to call triggerBuild() of the trigger '$triggerName'. Will retry")
            }
            true -> {
                val currentTime = myTriggerUtil.getCurrentTime()
                val name = context.triggerDescriptor.buildTriggerService.name
                context.buildType.addToQueue("$name $currentTime")

                TriggerUtil.setPreviousCallTime(currentTime, context)
            }
        }
    }

    private fun getOrCreateClient(onCreate: () -> Unit = {}): KtorClient =
        if (!this::myKtorClient.isInitialized || myKtorClient.outdated) {
            onCreate()
            KtorClient(host, port)
        } else myKtorClient

    override fun getPollInterval(ctx: PolledTriggerContext) = 30
}
