package jetbrains.buildServer.buildTriggers.remote

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.buildTriggers.async.AsyncPolledBuildTrigger
import jetbrains.buildServer.buildTriggers.remote.netty.Actions
import jetbrains.buildServer.buildTriggers.remote.netty.Client
import jetbrains.buildServer.buildTriggers.remote.netty.TriggerBuild
import jetbrains.buildServer.util.TimeService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

private const val host = "localhost"
private const val port = 8080

class RemoteTriggerPolicy(private val myTimeService: TimeService) : AsyncPolledBuildTrigger {
    private val myLogger = Logger.getInstance(RemoteTriggerPolicy::class.qualifiedName)
    private var myActionReference: AtomicReference<Actions?> = AtomicReference(null)

    private var myCurrVal: Int = 1
    private var myRequestSent = false

    override fun triggerActivated(context: PolledTriggerContext) = synchronized(this) {
        if (actionsIfNotOutdated() == null) {
            myLogger.debug("Trigger activation initialized a new connection")
            myActionReference.set(
                Client(
                    host,
                    port
                ).run())
        }
    }

    override fun triggerDeactivated(context: PolledTriggerContext) = synchronized(this) {
        actionsIfNotOutdated()?.closeConnection?.invoke()
        myActionReference.set(null)
    }

    override fun triggerBuild(prev: String?, context: PolledTriggerContext): String? = synchronized(this) {
        if (prev != null && prev == myCurrVal.toString()) return prev

        val actions = actionsIfNotOutdated() ?: run {
            myLogger.debug("triggerBuild() initialized a new connection")
            Client(
                host,
                port
            ).run()
        }
        if (actions == null) {
            myLogger.error("Could not send triggerBuild() request: no connection")
            return prev
        }
        myActionReference.set(actions)

        if (!myRequestSent) {
            val contextSubset = prepareContextSubset(context)
            actions.fireEvent(
                TriggerBuild(
                    contextSubset
                )
            )
            myRequestSent = true
        }

        val answer = actions.awaitRead(5, TimeUnit.SECONDS)
        if (answer == null) {
            myLogger.debug("Request timed out, will retry with next triggerBuild() invocation")
            myRequestSent = false
            return prev
        }

        if (answer) {
            val currentTime = getCurrentTime()
            val name = context.triggerDescriptor.buildTriggerService.name
            context.buildType.addToQueue("$name $currentTime")
            setPreviousCallTime(currentTime, context)
        }

        val rv = myCurrVal++
        myRequestSent = false
        return rv.toString()
    }

    private fun actionsIfNotOutdated() = myActionReference.updateAndGet {
        if (it?.outdated == true) {
            myLogger.debug("Outdated connection cleared")
            null
        } else it
    }

    private fun prepareContextSubset(context: PolledTriggerContext): Map<String, String> {
        val properties = context.triggerDescriptor.properties

        return mapOf(
            Constants.Request.ENABLE to getEnable(
                properties
            ).toString(),
            Constants.Request.DELAY to getDelay(
                properties
            ).toString(),
            Constants.Request.PREVIOUS_CALL_TIME to getPreviousCallTime(context).toString(),
            Constants.Request.CURRENT_TIME to getCurrentTime().toString()
        )
    }

    private fun setPreviousCallTime(time: Long, context: PolledTriggerContext) {
        context.customDataStorage.putValue(Constants.Request.PREVIOUS_CALL_TIME, time.toString())
    }

    private fun getPreviousCallTime(context: PolledTriggerContext) =
        context.customDataStorage
            .getValue(Constants.Request.PREVIOUS_CALL_TIME)
            ?.toLongOrNull()

    private fun getCurrentTime() = myTimeService.now()

    override fun getPollInterval(ctx: PolledTriggerContext) = 30
}
