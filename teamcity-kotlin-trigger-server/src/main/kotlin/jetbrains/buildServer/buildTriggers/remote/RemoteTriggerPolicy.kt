package jetbrains.buildServer.buildTriggers.remote

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.buildTriggers.async.AsyncPolledBuildTrigger
import jetbrains.buildServer.buildTriggers.remote.netty.Client
import jetbrains.buildServer.buildTriggers.remote.netty.TriggerBuild
import jetbrains.buildServer.util.TimeService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

private const val host = "localhost"
private const val port = 8080
private const val timeout = 10L
private val timeoutTimeUnit = TimeUnit.SECONDS

class RemoteTriggerPolicy(myTimeService: TimeService) : AsyncPolledBuildTrigger {
    private val myLogger = Logger.getInstance(RemoteTriggerPolicy::class.qualifiedName)
    private val myTriggerUtil = TriggerUtil(myTimeService)
    private var myActionReference: AtomicReference<Actions?> = AtomicReference(null)

    private var myCurrVal: Int = 1
    private var myRequestSent = false

    override fun triggerActivated(context: PolledTriggerContext) = synchronized(this) {
        if (actionsIfNotOutdated() == null) {
            myLogger.debug("Trigger activation initialized a new connection")
            myActionReference.set(
                Client(host, port).run()
            )
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
            Client(host, port).run()
        }
        if (actions == null) {
            myLogger.error("Could not send triggerBuild() request: no connection")
            return prev
        }
        myActionReference.set(actions)

        if (!myRequestSent) {
            val contextSubset = myTriggerUtil.contextSubset(context)
            actions.fireEvent(
                TriggerBuild(contextSubset)
            )
            myRequestSent = true
        }

        val answer = actions.awaitRead(myTriggerUtil.triggerId(context), timeout, timeoutTimeUnit)
        if (answer == null) {
            myLogger.debug("Request timed out, will retry with next triggerBuild() invocation")
            myRequestSent = false
            return prev
        }

        if (answer) {
            val currentTime = myTriggerUtil.getCurrentTime()
            val name = context.triggerDescriptor.buildTriggerService.name
            context.buildType.addToQueue("$name $currentTime")
            myTriggerUtil.setPreviousCallTime(currentTime, context)
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

    override fun getPollInterval(ctx: PolledTriggerContext) = 30
}
