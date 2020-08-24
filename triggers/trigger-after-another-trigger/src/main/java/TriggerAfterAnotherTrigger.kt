package jetbrains.buildServer.buildTriggers.remote.compiled

import io.ktor.client.features.ClientRequestException
import jetbrains.buildServer.buildTriggers.remote.CustomTriggerPolicy
import jetbrains.buildServer.buildTriggers.remote.Format
import jetbrains.buildServer.buildTriggers.remote.RestApiClient
import jetbrains.buildServer.buildTriggers.remote.TriggerContext
import jetbrains.buildServer.buildTriggers.remote.annotation.CustomTriggerProperties
import jetbrains.buildServer.buildTriggers.remote.annotation.CustomTriggerProperty
import jetbrains.buildServer.buildTriggers.remote.annotation.PropertyType
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat

@CustomTriggerProperties(
    CustomTriggerProperty("buildTypeId", PropertyType.TEXT, "Id of the build configuration to depend on", true),
    CustomTriggerProperty(
        "enableImmediately",
        PropertyType.BOOLEAN,
        "Trigger immediately after this trigger creation if all conditions are met"
    )
)
class TriggerAfterAnotherTrigger : CustomTriggerPolicy {

    override fun triggerBuild(context: TriggerContext, restApiClient: RestApiClient): Boolean {
        val enableImmediately = context.properties["enableImmediately"]?.toBoolean() ?: false
        val buildTypeId = context.properties["buildTypeId"]
            ?: throw IllegalArgumentException("This trigger has to receive an id of the build to depend on")

        val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmssZ")

        val lastBuildDateStr = context.customData["lastBuildDate"]
        val buildFinishDateStr = try {
            runBlocking {
                restApiClient.get("buildTypes/id:$buildTypeId/builds/state:finished/finishDate", Format.PlainText)
            }
        } catch (e: ClientRequestException) {
            return false
        }

        val lastBuildDate = lastBuildDateStr?.let { dateFormat.parse(it) }
        val buildFinishDate = dateFormat.parse(buildFinishDateStr)

        if (lastBuildDate?.before(buildFinishDate) != false) {
            context.customData["lastBuildDate"] = buildFinishDateStr
            return lastBuildDate != null || enableImmediately
        }

        return false
    }
}
