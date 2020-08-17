package jetbrains.buildServer.buildTriggers.remote

import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.serverSide.CustomDataStorage
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.util.TimeService

internal object TriggerUtil {

    fun createTriggerBuildContext(context: PolledTriggerContext, timeService: TimeService) = TriggerContext(
        timeService.now(),
        parseTriggerAdditionalProperties(context.triggerDescriptor.properties) ?: emptyMap(),
        getCustomDataStorageOfTrigger(context).values?.toMutableMap() ?: mutableMapOf(),
        context.buildType.convert()
    )

    fun parseTriggerAdditionalProperties(properties: Map<String, String>): Map<String, String>? {
        return properties[Constants.ADDITIONAL_PROPERTIES].orEmpty()
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

    private fun SBuildType.convert(): BuildType {
        val project = Project(project.externalId, project.isArchived)

        val history = history.map {
            FinishedBuild(it.finishDate, it.convert())
        }

        val lastChangesStartedBuild = lastChangesStartedBuild?.convert()
        val lastChangesSuccessfullyFinished = lastChangesSuccessfullyFinished?.convert()
        val lastChangesFinished = lastChangesFinished?.convert()

        return BuildType(
            externalId,
            isInQueue,
            isPaused,
            project,
            history,
            lastChangesStartedBuild,
            lastChangesSuccessfullyFinished,
            lastChangesFinished,
            buildParameters,
            parameters,
            tags
        )
    }

    private fun SBuild.convert() = Build(
        buildId,
        startDate,
        isFinished,
        duration
    )
}