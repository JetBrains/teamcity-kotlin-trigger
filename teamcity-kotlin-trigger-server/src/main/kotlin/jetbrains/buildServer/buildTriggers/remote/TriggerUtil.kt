package jetbrains.buildServer.buildTriggers.remote

import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.util.TimeService
import java.io.File

internal class TriggerUtil(private val myTimeService: TimeService) {
    fun createTriggerBuildRequest(context: PolledTriggerContext): TriggerBuildRequest {
        val properties = context.triggerDescriptor.properties
        return TriggerBuildRequest(
            getEnable(properties),
            getDelay(properties)!!,
            getPreviousCallTime(context),
            getCurrentTime()
        )
    }

    fun getCurrentTime() = myTimeService.now()

    companion object {
        fun getEnable(properties: Map<String, String>) = StringUtil.isTrue(properties[Constants.ENABLE])
        fun getDelay(properties: Map<String, String>) = properties[Constants.DELAY]?.toIntOrNull()

        fun getTriggerId(context: PolledTriggerContext): String = context.triggerDescriptor.id

        fun getTargetTriggerPath(context: PolledTriggerContext): String =
            context.triggerDescriptor.properties[Constants.TRIGGER_POLICY]!!

        fun getTargetTriggerName(context: PolledTriggerContext): String =
            File(getTargetTriggerPath(context)).nameWithoutExtension

        fun setPreviousCallTime(time: Long, context: PolledTriggerContext) {
            context.customDataStorage.putValue(Constants.PREVIOUS_CALL_TIME, time.toString())
        }

        private fun getPreviousCallTime(context: PolledTriggerContext) =
            context.customDataStorage
                .getValue(Constants.PREVIOUS_CALL_TIME)
                ?.toLongOrNull()
    }
}