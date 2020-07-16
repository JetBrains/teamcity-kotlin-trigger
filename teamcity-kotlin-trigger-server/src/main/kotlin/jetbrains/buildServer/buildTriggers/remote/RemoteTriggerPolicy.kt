package jetbrains.buildServer.buildTriggers.remote

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.buildTriggers.async.AsyncPolledBuildTrigger
import jetbrains.buildServer.buildTriggers.remote.ktor.KtorClient
import jetbrains.buildServer.util.TimeService

private const val host = "localhost"
private const val port = 8080

class RemoteTriggerPolicy(myTimeService: TimeService) : AsyncPolledBuildTrigger {
    private val myLogger = Logger.getInstance(RemoteTriggerPolicy::class.qualifiedName)
    private val myTriggerUtil = TriggerUtil(myTimeService)
    private lateinit var myKtorClient: KtorClient

    private var myCurrVal: Int = 1

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
        if (prev != null && prev == myCurrVal.toString()) return prev

        val client = getOrCreateClient {
            myLogger.debug("triggerBuild() initialized a new connection")
        }

        val contextSubset = myTriggerUtil.contextSubset(context)

        when (client.sendTriggerBuild(contextSubset)) {
            null -> {
                myLogger.debug("Request failed, will retry with next triggerBuild() invocation")
                return prev
            }
            true -> {
                val currentTime = myTriggerUtil.getCurrentTime()
                val name = context.triggerDescriptor.buildTriggerService.name
                context.buildType.addToQueue("$name $currentTime")
                myTriggerUtil.setPreviousCallTime(currentTime, context)
            }
        }

        return (myCurrVal++).toString()
    }

    private fun getOrCreateClient(onCreate: () -> Unit = {}): KtorClient = synchronized(this) {
        if (!this::myKtorClient.isInitialized || myKtorClient.outdated) {
            onCreate()
            KtorClient(host, port)
        } else myKtorClient
    }

    override fun getPollInterval(ctx: PolledTriggerContext) = 30
}
