package com.jetbrains.teamcity.kotlin.trigger.netty

import com.jetbrains.teamcity.kotlin.trigger.*
import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import java.util.*

internal sealed class Event(val params: Map<String, String>)

internal class TriggerBuild(params: Map<String, String>) : Event(params) {
    companion object {
        fun prepareContextSubset(context: PolledTriggerContext): Map<String, String> {
            val properties = context.triggerDescriptor.properties

            val enable = getEnable(properties)
            val delay = getDelay(properties)
            val prevCallTime = getPreviousCallTime(context)

            return mapOf(
                    ENABLE to enable.toString(),
                    DELAY to delay.toString(),
                    PREVIOUS_CALL_TIME to prevCallTime.toString(),
                    CURRENT_TIME to Date().time.toString()
            )
        }
    }
}