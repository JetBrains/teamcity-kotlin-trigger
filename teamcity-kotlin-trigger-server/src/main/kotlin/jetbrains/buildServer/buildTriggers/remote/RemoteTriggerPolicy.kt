package jetbrains.buildServer.buildTriggers.remote

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.buildTriggers.async.AsyncPolledBuildTrigger
import jetbrains.buildServer.buildTriggers.remote.ktor.KtorClient
import jetbrains.buildServer.util.TimeService
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

private const val host = "localhost"
private const val port = 8080

private enum class State {
    UploadTrigger, TriggerBuild
}

class RemoteTriggerPolicy(myTimeService: TimeService, private val myExecutor: ExecutorService) :
    AsyncPolledBuildTrigger {
    private val myLogger = Logger.getInstance(RemoteTriggerPolicy::class.qualifiedName)
    private val myTriggerUtil = TriggerUtil(myTimeService)
    private lateinit var myKtorClient: KtorClient

    private val myStateMap = mutableMapOf<String, State>()

    override fun triggerActivated(context: PolledTriggerContext) = synchronized(this) {
        myKtorClient = getOrCreateClient {
            myLogger.debug("Trigger activation initialized a new connection")
        }
        // In future, trigger activation may mean that the trigger has changed, so the initial state should become UploadTrigger
        myStateMap[TriggerUtil.getTriggerId(context)] = State.TriggerBuild
    }

    override fun triggerDeactivated(context: PolledTriggerContext): Unit = synchronized(this) {
        if (this::myKtorClient.isInitialized && !myKtorClient.outdated)
            myKtorClient.closeConnection()
    }

    override fun triggerBuild(prev: String?, context: PolledTriggerContext): String? {
        val id = TriggerUtil.getTriggerId(context)

        val triggerPath = TriggerUtil.getTargetTriggerPath(context.triggerDescriptor.properties) ?: run {
            myLogger.debug("Trigger not specified, triggerBuild() invocation skipped")
            return myStateMap[id].toString()
        }

        return synchronized(id) {
            when (myStateMap[id]) {
                State.TriggerBuild -> triggerBuild(triggerPath, context)
                State.UploadTrigger -> uploadTrigger(triggerPath, context)
            }
            myStateMap[id].toString()
        }
    }

    private fun uploadTrigger(triggerPath: String, context: PolledTriggerContext, shouldTriggerBuild: Boolean = false) {
        val id = TriggerUtil.getTriggerId(context)
        val triggerName = TriggerUtil.getTriggerName(triggerPath)

        completableFutureAsync {
            val client = getOrCreateClient {
                myLogger.debug("UploadTrigger action initialized a new connection")
            }
            val triggerBytes = File(triggerPath).readBytes()
            client.uploadTrigger(triggerName, UploadTriggerRequest(triggerBytes))
        }.whenComplete { result, completionException ->
            when (val exception = completionException?.cause) {
                null -> {
                    if (!result)
                        myLogger.error("Failed to upload trigger '$triggerName' to the server. Will retry")
                    else {
                        myStateMap[id] = State.TriggerBuild
                        if (shouldTriggerBuild) {
                            myLogger.debug("UploadTrigger action succeeded and invoked TriggerBuild")
                            triggerBuild(triggerPath, context, false)
                        }
                    }
                }
                is ServerError -> myLogger.error("Server responded with an error: $exception")
                else -> myLogger.error("Unknown exception", exception)
            }
        }
    }

    private fun triggerBuild(triggerPath: String, context: PolledTriggerContext, shouldTryUpload: Boolean = true) {
        val id = TriggerUtil.getTriggerId(context)
        val triggerName = TriggerUtil.getTriggerName(triggerPath)

        completableFutureAsync {
            myLogger.info("RUNNING WITH ${Thread.currentThread().name}")
            val client = getOrCreateClient {
                myLogger.debug("TriggerBuild action initialized a new connection")
            }
            val triggerBuildRequest = myTriggerUtil.createTriggerBuildRequest(context)
            client.sendTriggerBuild(triggerName, triggerBuildRequest)
        }.handle { result, completionException ->
            when (val exception = completionException?.cause) {
                null -> result
                is TriggerDoesNotExistError -> {
                    myLogger.warn(exception.message ?: "Trigger '$triggerName' does not exist on the server")
                    myStateMap[id] = State.UploadTrigger
                    if (shouldTryUpload) {
                        myLogger.debug("TriggerBuild action failed and invoked UploadTrigger")
                        uploadTrigger(triggerPath, context, true)
                    }
                    false
                }
                is ServerError -> {
                    myLogger.error("Server responded with an error: $exception")
                    null
                }
                else -> {
                    myLogger.error("Unknown exception", exception)
                    null
                }
            }
        }.thenAccept {
            when (it) {
                null -> myLogger.debug("Failed to call TriggerBuild action of the trigger '$triggerName'. Will retry")
                true -> {
                    val currentTime = myTriggerUtil.getCurrentTime()
                    val name = context.triggerDescriptor.buildTriggerService.name
                    context.buildType.addToQueue("$name $currentTime")

                    TriggerUtil.setPreviousCallTime(currentTime, context)
                }
            }
        }
    }

    private fun getOrCreateClient(onCreate: () -> Unit = {}): KtorClient =
        if (!this::myKtorClient.isInitialized || myKtorClient.outdated) {
            onCreate()
            KtorClient(host, port)
        } else myKtorClient

    private fun <T> completableFutureAsync(supplier: () -> T) =
        CompletableFuture.supplyAsync(supplier, myExecutor::execute)

    override fun getPollInterval(ctx: PolledTriggerContext) = 30
}
