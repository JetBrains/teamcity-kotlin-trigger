package com.jetbrains.teamcity.boris.simplePlugin

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor
import jetbrains.buildServer.buildTriggers.BuildTriggerService
import jetbrains.buildServer.buildTriggers.async.AsyncPolledBuildTriggerFactory
import jetbrains.buildServer.log.Loggers
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.web.openapi.PluginDescriptor

class RemoteTriggerService(private val myPluginDescriptor: PluginDescriptor,
                           factory: AsyncPolledBuildTriggerFactory) :
        BuildTriggerService() {
    private val myPolicy = factory.createBuildTrigger(RemoteTriggerPolicy(),
            Logger.getInstance(Loggers.SERVER_CATEGORY + RemoteTriggerService::class))

    override fun getName() = "simpleTrigger"

    override fun getDisplayName() = "A simple scheduling trigger"

    override fun describeTrigger(buildTriggerDescriptor: BuildTriggerDescriptor): String {
        val properties = buildTriggerDescriptor.properties
        if (!getEnable(properties)) return "Does nothing (edit configurations to activate it)"

        val delay = getDelay(properties)
        if (delay == null || delay <= 0) return "Incorrect state: wrong delay"

        val period = StringBuilder("minute")

        if (delay % 10 != 1) period.append("s")
        if (delay != 1) period.insert(0, " ").insert(0, delay)

        return "Initiates a build every $period"
    }

    override fun getTriggerPropertiesProcessor() =
            PropertiesProcessor { properties: Map<String, String> ->
                val enable = getEnable(properties)
                val delay = getDelay(properties)

                if (enable && (delay == null || delay <= 0)) {
                    return@PropertiesProcessor listOf(InvalidProperty(DELAY, "Specify a correct delay, please"))
                }
                emptyList()
            }

    override fun getEditParametersUrl() = myPluginDescriptor.getPluginResourcesPath("simpleTrigger.jsp")

    override fun isMultipleTriggersPerBuildTypeAllowed() = true

    override fun getBuildTriggeringPolicy() = myPolicy
}

internal const val ENABLE = "enable"
internal const val DELAY = "delay"
internal const val PREVIOUS_CALL_TIME = "previousCallTime"
internal const val CURRENT_TIME = "currentTime"

internal fun getEnable(properties: Map<String, String>) = StringUtil.isTrue(properties[ENABLE])

internal fun getDelay(properties: Map<String, String>) =
        try {
            properties[DELAY]?.toInt()
        } catch (e: NumberFormatException) {
            null
        }