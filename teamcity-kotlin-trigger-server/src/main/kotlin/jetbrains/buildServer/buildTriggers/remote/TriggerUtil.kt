package jetbrains.buildServer.buildTriggers.remote

import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.util.TimeService
import java.io.File

internal class TriggerUtil(private val myTimeService: TimeService) {
    fun triggerId(context: PolledTriggerContext): String = context.triggerDescriptor.id

    fun contextSubset(context: PolledTriggerContext): Map<String, String> {
        val properties = context.triggerDescriptor.properties

        return mapOf(
//            Constants.TRIGGER_ID to triggerId(context),
            Constants.Request.ENABLE to getEnable(properties).toString(),
            Constants.Request.DELAY to getDelay(properties).toString(),
            Constants.Request.PREVIOUS_CALL_TIME to getPreviousCallTime(context).toString(),
            Constants.Request.CURRENT_TIME to getCurrentTime().toString()
        )
    }

    fun getTargetTriggerNameAndPath(context: PolledTriggerContext): Pair<String, String> {
        val properties = context.triggerDescriptor.properties
        val path = properties["triggerPolicy"]!!
        val name = File(path).nameWithoutExtension
        return name to path
    }

    companion object {
        fun getEnable(properties: Map<String, String>) = StringUtil.isTrue(properties[Constants.Request.ENABLE])
        fun getDelay(properties: Map<String, String>) = properties[Constants.Request.DELAY]?.toIntOrNull()
    }

    fun getCurrentTime() = myTimeService.now()

    fun setPreviousCallTime(time: Long, context: PolledTriggerContext) {
        context.customDataStorage.putValue(Constants.Request.PREVIOUS_CALL_TIME, time.toString())
    }

    private fun getPreviousCallTime(context: PolledTriggerContext) =
        context.customDataStorage
            .getValue(Constants.Request.PREVIOUS_CALL_TIME)
            ?.toLongOrNull()
}