package jetbrains.buildServer.buildTriggers.remote

import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.util.TimeService

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

    fun getCurrentTime() = myTimeService.now()

    fun setPreviousCallTime(time: Long, context: PolledTriggerContext) {
        context.customDataStorage.putValue(Constants.Request.PREVIOUS_CALL_TIME, time.toString())
    }

    private fun getPreviousCallTime(context: PolledTriggerContext) =
        context.customDataStorage
            .getValue(Constants.Request.PREVIOUS_CALL_TIME)
            ?.toLongOrNull()
}