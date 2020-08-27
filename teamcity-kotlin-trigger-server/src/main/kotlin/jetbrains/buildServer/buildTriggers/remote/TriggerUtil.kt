package jetbrains.buildServer.buildTriggers.remote

import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.serverSide.CustomDataStorage
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.util.TimeService

private const val RUNNING_BUILD_AMOUNT = 20
private const val HISTORY_SIZE = 20

internal object TriggerUtil {

    fun createTriggerBuildContext(context: PolledTriggerContext, timeService: TimeService) = TriggerContext(
        timeService.now(),
        getCombinedProperties(context.triggerDescriptor.properties) ?: emptyMap(),
        getCustomDataStorageOfTrigger(context).values?.toMutableMap() ?: mutableMapOf(),
        context.buildType.convert()
    )

    fun getCombinedProperties(properties: Map<String, String>): Map<String, String>? {
        val declaredProperties = properties.toMutableMap()
        val additionalPropertiesStr = declaredProperties.remove(Constants.ADDITIONAL_PROPERTIES)

        return declaredProperties + (parseAdditionalProperties(additionalPropertiesStr) ?: return null)
    }

    private fun parseAdditionalProperties(additionalProperties: String?): Map<String, String>? {
        return additionalProperties.orEmpty()
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

        val runningBuilds = runningBuilds.take(RUNNING_BUILD_AMOUNT)
            .map { RunningBuild(it.convert(), it.agentId) }

        val history = history.take(HISTORY_SIZE)
            .map { FinishedBuild(it.convert(), it.finishDate) }

        val lastChangesStartedBuild = lastChangesStartedBuild?.convert()
        val lastChangesSuccessfullyFinished = lastChangesSuccessfullyFinished?.convert()
        val lastChangesFinished = lastChangesFinished?.convert()

        return BuildType(
            externalId,
            isInQueue,
            isPaused,
            project,
            runningBuilds,
            history,
            lastChangesStartedBuild,
            lastChangesSuccessfullyFinished,
            lastChangesFinished,
            buildParameters,
            parameters
        )
    }

    private fun SBuild.convert() = Build(
        buildId,
        startDate,
        isFinished,
        duration
    )
}