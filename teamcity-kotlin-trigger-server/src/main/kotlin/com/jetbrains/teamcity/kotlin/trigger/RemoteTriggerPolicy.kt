package com.jetbrains.teamcity.kotlin.trigger

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.teamcity.kotlin.trigger.netty.Actions
import com.jetbrains.teamcity.kotlin.trigger.netty.Client
import com.jetbrains.teamcity.kotlin.trigger.netty.TriggerBuild
import jetbrains.buildServer.buildTriggers.BuildTriggerException
import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.buildTriggers.async.AsyncPolledBuildTrigger
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

const val host = "localhost"
const val port = 8080

class RemoteTriggerPolicy : AsyncPolledBuildTrigger {
    private var actionReference: AtomicReference<Actions?> = AtomicReference(null)

    override fun triggerActivated(context: PolledTriggerContext) = synchronized(this) {
        if (actionReference.get() == null) {
            logDebugIfEnabled(createContextLogger(context), "Trigger activation initialized a new connection")
            actionReference.compareAndSet(null, Client(host, port).run())
        }
    }

    override fun triggerDeactivated(context: PolledTriggerContext) = synchronized(this) {
        actionReference.get()?.closeConnection?.invoke()
        actionReference.set(null)
    }

    private var currVal: Int = 1
    private var requestSent = false

    @Throws(BuildTriggerException::class)
    override fun triggerBuild(prev: String?, context: PolledTriggerContext): String? = synchronized(this) {
        if (prev != null && prev == currVal.toString()) return prev

        val contextLogger = createContextLogger(context)

        val actions = actionReference.get() ?: run {
            logDebugIfEnabled(contextLogger, "triggerBuild() initialized a new connection")
            Client(host, port).run()
        }
        actionReference.compareAndSet(null, actions)

        if (!requestSent) {
            val contextSubset = TriggerBuild.prepareContextSubset(context)
            actions.fireEvent(TriggerBuild(contextSubset))
            requestSent = true
        }

        val answer = actions.awaitRead(5, TimeUnit.SECONDS)
        if (answer == null) {
            logDebugIfEnabled(createContextLogger(context),
                    "Request timed out, will retry with next triggerBuild() invocation")
            requestSent = false
            return prev
        }

        if (answer) {
            val currentDate = Date().time
            val name = context.triggerDescriptor.buildTriggerService.name
            context.buildType.addToQueue("$name $currentDate")
            setPreviousCallTime(currentDate, context)
        }

        val rv = currVal
        currVal = (currVal + 1) % Int.MAX_VALUE
        requestSent = false
        return rv.toString()
    }

    override fun getPollInterval(p0: PolledTriggerContext): Int {
        return 30
    }

    private fun createContextLogger(ctx: PolledTriggerContext): Logger =
            Logger.getInstance(RemoteTriggerPolicy::class.qualifiedName + "_" + ctx.buildType.externalId)

    private fun logDebugIfEnabled(logger: Logger, msg: String) {
        if (logger.isDebugEnabled)
            logger.debug(msg)
    }
}

private fun setPreviousCallTime(time: Long, context: PolledTriggerContext) {
    context.customDataStorage.putValue(PREVIOUS_CALL_TIME, time.toString())
}

internal fun getPreviousCallTime(context: PolledTriggerContext) =
        try {
            val previousCallTimeStr = context.customDataStorage.getValue(PREVIOUS_CALL_TIME)
            previousCallTimeStr?.toLong()
        } catch (e: NumberFormatException) {
            null
        }
