package jetbrains.buildServer.buildTriggers.remote

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor
import jetbrains.buildServer.buildTriggers.BuildTriggerService
import jetbrains.buildServer.buildTriggers.async.AsyncPolledBuildTriggerFactory
import jetbrains.buildServer.buildTriggers.remote.controller.CUSTOM_TRIGGERS_LIST_CONTROLLER
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.util.TimeService
import jetbrains.buildServer.web.openapi.PluginDescriptor
import org.springframework.stereotype.Service

@Service
class CustomTriggerService(
    private val myPluginDescriptor: PluginDescriptor,
    factory: AsyncPolledBuildTriggerFactory,
    timeService: TimeService,
    private val myCustomTriggersBean: CustomTriggersManager
) : BuildTriggerService() {

    private val myPolicy = factory.createBuildTrigger(
        RemoteTriggerPolicy(timeService, myCustomTriggersBean),
        Logger.getInstance(CustomTriggerService::class.qualifiedName)
    )

    override fun getName() = "teamcityKotlinTrigger"
    override fun getDisplayName() = "Custom Trigger..."

    override fun describeTrigger(buildTriggerDescriptor: BuildTriggerDescriptor): String {
        val properties = buildTriggerDescriptor.properties
        val triggerPolicyPath = TriggerUtil.getTargetTriggerPolicyPath(properties)
            ?: return "Trigger policy is not selected"

        val disabledStatus =
            if (myCustomTriggersBean.isTriggerPolicyEnabled(triggerPolicyPath)) ""
            else "(disabled)"

        val triggerPolicyName = CustomTriggerPolicyDescriptor.policyPathToPolicyName(triggerPolicyPath)

        return "Uses $triggerPolicyName $disabledStatus"
    }

    override fun getTriggerPropertiesProcessor() = PropertiesProcessor { properties: Map<String, String> ->
        val triggerPolicy = TriggerUtil.getTargetTriggerPolicyPath(properties)
        val triggerProperties = TriggerUtil.parseTriggerProperties(properties)

        val rv = mutableListOf<InvalidProperty>()

        if (triggerPolicy.isNullOrBlank())
            rv.add(InvalidProperty(Constants.TRIGGER_POLICY_PATH, "A trigger policy should be specified"))

        if (triggerProperties == null)
            rv.add(InvalidProperty(Constants.PROPERTIES, "Properties should be 'key=value' pairs on separate lines"))

        rv
    }

    override fun getEditParametersUrl() =
        myPluginDescriptor.getPluginResourcesPath(CUSTOM_TRIGGERS_LIST_CONTROLLER)

    override fun getBuildTriggeringPolicy() = myPolicy
    override fun isMultipleTriggersPerBuildTypeAllowed() = true
}
