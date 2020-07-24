package jetbrains.buildServer.buildTriggers.remote

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor
import jetbrains.buildServer.buildTriggers.BuildTriggerService
import jetbrains.buildServer.buildTriggers.async.AsyncPolledBuildTriggerFactory
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.util.TimeService
import jetbrains.buildServer.web.openapi.PluginDescriptor

class RemoteTriggerService(
    private val myPluginDescriptor: PluginDescriptor,
    factory: AsyncPolledBuildTriggerFactory,
    timeService: TimeService
) : BuildTriggerService() {

    private val myPolicy = factory.createBuildTrigger(
        RemoteTriggerPolicy(timeService),
        Logger.getInstance(RemoteTriggerService::class.qualifiedName)
    )

    override fun getName() = "teamcityKotlinTrigger"
    override fun getDisplayName() = "Remote triggers"

    override fun describeTrigger(buildTriggerDescriptor: BuildTriggerDescriptor): String {
        val properties = buildTriggerDescriptor.properties
        val triggerPolicyPath = TriggerUtil.getTargetTriggerPolicyPath(properties)
            ?: return "Trigger policy is not selected"

        return "Uses ${TriggerUtil.getTriggerPolicyName(triggerPolicyPath)}"
    }

    override fun getTriggerPropertiesProcessor() =
        PropertiesProcessor { properties: Map<String, String> ->
            val triggerPolicy = TriggerUtil.getTargetTriggerPolicyPath(properties)
            val triggerProperties = TriggerUtil.parseTriggerProperties(properties)

            val rv = mutableListOf<InvalidProperty>()

            if (triggerPolicy.isNullOrBlank())
                rv.add(InvalidProperty(Constants.TRIGGER_POLICY, "A trigger policy should be specified"))
            if (triggerProperties == null)
                rv.add(InvalidProperty(Constants.PROPERTIES, "Properties should be 'key=value' pairs on separate lines"))

            rv
        }

    override fun getEditParametersUrl() = myPluginDescriptor.getPluginResourcesPath("remoteTriggerController.html")

    override fun isMultipleTriggersPerBuildTypeAllowed() = true
    override fun getBuildTriggeringPolicy() = myPolicy
}
