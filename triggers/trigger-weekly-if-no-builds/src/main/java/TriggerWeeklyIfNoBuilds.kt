package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.CustomTriggerPolicy
import jetbrains.buildServer.buildTriggers.remote.RestApiClient
import jetbrains.buildServer.buildTriggers.remote.TriggerContext
import jetbrains.buildServer.buildTriggers.remote.annotation.CustomTriggerProperty
import jetbrains.buildServer.buildTriggers.remote.annotation.PropertyType
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@CustomTriggerProperty("weekday", PropertyType.TEXT, "Day of the week, defaults to Wednesday", false)
class TriggerWeeklyIfNoBuilds : CustomTriggerPolicy {

    override fun triggerBuild(context: TriggerContext, restApiClient: RestApiClient): Boolean {
        val buildType = context.buildType
        if (buildType.isInQueue || buildType.runningBuilds.isNotEmpty()) return false

        if (!isDesiredDayOfWeek(context)) return false

        val dateTooOld = Instant.ofEpochMilli(context.currentTime)
            .minus(7, ChronoUnit.DAYS) // ChronoUnit.WEEKS is unsupported

        val mostRecentFinishedBuild = buildType.history.firstOrNull()

        val isRecentEnough = mostRecentFinishedBuild?.build
            ?.startDate
            ?.toInstant()
            ?.isAfter(dateTooOld)

        return isRecentEnough != true
    }

    private fun isDesiredDayOfWeek(context: TriggerContext): Boolean {
        val weekDayStr = context.properties["weekday"]?.trim() ?: DayOfWeek.WEDNESDAY.toString()

        val desiredDayOfWeek = try {
            SimpleDateFormat("EEE").parse(weekDayStr).dayOfWeek
        } catch (e: ParseException) {
            throw IllegalArgumentException("Property '$weekDayStr' does not represent a day of the week")
        }

        return desiredDayOfWeek == Date(context.currentTime).dayOfWeek
    }

    private val Date.dayOfWeek: Int
        get() = Calendar.getInstance().also {
            it.time = this
        }.get(Calendar.DAY_OF_WEEK)
}
