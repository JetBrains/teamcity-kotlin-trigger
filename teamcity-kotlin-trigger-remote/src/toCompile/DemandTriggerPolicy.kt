package jetbrains.buildServer.buildTriggers.remote.compiled

import jetbrains.buildServer.buildTriggers.remote.*
import jetbrains.buildServer.buildTriggers.remote.annotation.*

@CustomTriggerProperty("enable", PropertyType.BOOLEAN, "Enable the trigger", false)
class DemandTriggerPolicy : CustomTriggerPolicy {
    override fun triggerBuild(context: TriggerContext, restApiClient: RestApiClient) =
        context.properties["enable"]?.toBoolean() ?: false
}