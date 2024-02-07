

package jetbrains.buildServer.buildTriggers.remote

import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.serverSide.CustomDataStorage
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.util.TimeService

private const val RUNNING_BUILD_AMOUNT = 20
private const val HISTORY_SIZE = 20

internal object TriggerUtil {
    const val TRIGGER_POLICY_NAME = "triggerPolicy"

    fun createTriggerBuildContext(context: PolledTriggerContext, timeService: TimeService) = TriggerContext(
        timeService.now(),
        context.triggerDescriptor.properties,
        getCustomDataStorageOfTrigger(context).values?.toMutableMap() ?: mutableMapOf(),
        context.buildType.convert()
    )

    fun getCustomDataStorageOfTrigger(context: PolledTriggerContext): CustomDataStorage {
        val triggerServiceId = context.triggerDescriptor.buildTriggerService::class.qualifiedName
        val triggerId = context.triggerDescriptor.id

        return context.buildType.getCustomDataStorage(triggerServiceId + "_" + triggerId)
    }

    fun getTargetTriggerPolicyName(properties: Map<String, String>): String? =
        properties[TRIGGER_POLICY_NAME]

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