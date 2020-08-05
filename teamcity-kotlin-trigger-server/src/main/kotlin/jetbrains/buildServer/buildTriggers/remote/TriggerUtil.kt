package jetbrains.buildServer.buildTriggers.remote

import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.serverSide.CustomDataStorage
import jetbrains.buildServer.util.TimeService

internal object TriggerUtil {

    fun createTriggerBuildContext(context: PolledTriggerContext, timeService: TimeService) = TriggerContext(
        timeService.now(),
        parseTriggerProperties(context.triggerDescriptor.properties) ?: emptyMap(),
        getCustomDataStorageOfTrigger(context).values?.toMutableMap() ?: mutableMapOf()
    )

    fun parseTriggerProperties(properties: Map<String, String>): Map<String, String>? {
        return properties[Constants.PROPERTIES].orEmpty()
            .lines()
            .map { it.trim() }
            .filterNot { it.isEmpty() }
            .map {
                val i = it.indexOf("=")
                if (i <= 0 || i == it.length - 1) return null
                it.substring(0, i).trim() to it.substring(i + 1).trim()
            }.toMap()
    }

    fun getCustomDataStorageOfTrigger(context: PolledTriggerContext): CustomDataStorage {
        val triggerServiceId = context.triggerDescriptor.buildTriggerService::class.qualifiedName
        val triggerId = context.triggerDescriptor.id

        return context.buildType.getCustomDataStorage(triggerServiceId + "_" + triggerId)
    }

    fun getTargetTriggerPolicyPath(properties: Map<String, String>): String? =
        properties[Constants.TRIGGER_POLICY_PATH]
}