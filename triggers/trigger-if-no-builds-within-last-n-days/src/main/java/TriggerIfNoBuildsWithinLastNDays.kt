package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.CustomTriggerPolicy
import jetbrains.buildServer.buildTriggers.remote.RestApiClient
import jetbrains.buildServer.buildTriggers.remote.TriggerContext
import jetbrains.buildServer.buildTriggers.remote.annotation.CustomTriggerProperty
import jetbrains.buildServer.buildTriggers.remote.annotation.PropertyType
import java.time.Instant
import java.time.temporal.ChronoUnit

@CustomTriggerProperty(
    "daysNumber",
    PropertyType.TEXT,
    "How many days without builds it should take to trigger a build",
    true
)
class TriggerIfNoBuildsWithinLastNDays : CustomTriggerPolicy {

    override fun triggerBuild(context: TriggerContext, restApiClient: RestApiClient): Boolean {
        val buildType = context.buildType
        if (buildType.isInQueue || buildType.runningBuilds.isNotEmpty()) return false

        val nDays = getAndValidateDaysNumber(context.properties)

        val now = Instant.ofEpochMilli(context.currentTime)
        val dateTooOld = now.minus(nDays.toLong(), ChronoUnit.DAYS)

        val mostRecentFinishedBuild = buildType.history.firstOrNull()
        val mostRecentFinishedBuildIsRecentEnough =
            mostRecentFinishedBuild?.build?.startDate?.toInstant()?.isAfter(dateTooOld)

        return mostRecentFinishedBuildIsRecentEnough != true
    }

    private fun getAndValidateDaysNumber(properties: Map<String, String>): Int {
        val daysNumber = properties["daysNumber"]?.toIntOrNull()
            ?: throw IllegalArgumentException("This trigger has to receive an integer as a 'daysNumber' property")

        if (daysNumber < 1) throw java.lang.IllegalArgumentException("'daysNumber' property must be positive")

        return daysNumber
    }
}