package com.jetbrains.teamcity.boris.simplePlugin

import jetbrains.buildServer.buildTriggers.*
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.web.openapi.PluginDescriptor
import java.util.*

class SimpleTrigger(private val myPluginDescriptor: PluginDescriptor) : BuildTriggerService() {
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
                    return@PropertiesProcessor listOf(InvalidProperty(DELAY_PROPERTY, "Specify a correct delay, please"))
                }
                emptyList()
            }

    override fun getDefaultTriggerProperties() = hashMapOf(
            ENABLE_PROPERTY to "true",
            DELAY_PROPERTY to "10"
    )

    override fun getEditParametersUrl() = myPluginDescriptor.getPluginResourcesPath("simpleTrigger.jsp")

    override fun isMultipleTriggersPerBuildTypeAllowed() = true

    override fun getBuildTriggeringPolicy() = object : PolledBuildTrigger() {

        @Throws(BuildTriggerException::class)
        override fun triggerBuild(context: PolledTriggerContext) {
            val properties = context.triggerDescriptor.properties
            val delay = getDelay(properties)

            if (!getEnable(properties) || null == delay) return

            val currentDate = Date().time
            val previousCallTime = getPreviousCallTime(context)

            if (null == previousCallTime || currentDate - previousCallTime >= delay * 60000) {
                context.buildType.addToQueue("$name $currentDate")
                setPreviousCallTime(currentDate, context)
            }
        }
    }

    private fun setPreviousCallTime(time: Long, context: PolledTriggerContext) {
        context.customDataStorage.putValue(PREVIOUS_CALL_TIME, time.toString())
    }

    private fun getPreviousCallTime(context: PolledTriggerContext) =
            try {
                val previousCallTimeStr = context.customDataStorage.getValue(PREVIOUS_CALL_TIME)
                previousCallTimeStr?.toLong()
            } catch (e: NumberFormatException) {
                null
            }

    private fun getEnable(properties: Map<String, String>) = StringUtil.isTrue(properties[ENABLE_PROPERTY])

    private fun getDelay(properties: Map<String, String>) =
            try {
                properties[DELAY_PROPERTY]?.toInt()
            } catch (e: NumberFormatException) {
                null
            }

    companion object {
        const val ENABLE_PROPERTY = "enable"
        const val DELAY_PROPERTY = "delay"
        private const val PREVIOUS_CALL_TIME = "previousCallTime"
    }

}