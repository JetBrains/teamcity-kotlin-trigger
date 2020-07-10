package com.jetbrains.teamcity.boris.simplePlugin

import com.jetbrains.teamcity.boris.simplePlugin.netty.*
import jetbrains.buildServer.buildTriggers.BuildTriggerException
import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.buildTriggers.async.AsyncPolledBuildTrigger
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class RemoteTriggerPolicy : AsyncPolledBuildTrigger {
    private var actionReference: AtomicReference<Actions?> = AtomicReference(null)

    override fun triggerActivated(context: PolledTriggerContext) = synchronized(this) {
        if (actionReference.get() == null)
            actionReference.compareAndSet(null, Client("localhost", 8080).run())
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

        val actions = actionReference.get() ?: Client("localhost", 8080).run()
        actionReference.compareAndSet(null, actions)

        if (!requestSent) {
            val contextSubset = TriggerBuild.prepareContextSubset(context)
            actions.fireEvent(TriggerBuild(contextSubset))
            requestSent = true
        }

        val answer = actions.awaitRead(5, TimeUnit.SECONDS)
        if (answer == null) {
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
