package jetbrains.buildServer.buildTriggers.remote

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor
import jetbrains.buildServer.buildTriggers.BuildTriggerService
import jetbrains.buildServer.buildTriggers.async.AsyncPolledBuildTriggerFactory
import jetbrains.buildServer.buildTriggers.remote.controller.CUSTOM_TRIGGERS_LIST_CONTROLLER
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.util.TimeService
import jetbrains.buildServer.web.openapi.PluginDescriptor
import org.springframework.stereotype.Service

@Service
class CustomTriggerService(
    factory: AsyncPolledBuildTriggerFactory,
    timeService: TimeService,
    private val myPluginDescriptor: PluginDescriptor,
    private val myProjectManager: ProjectManager,
    private val myCustomTriggersManager: CustomTriggersManager
) : BuildTriggerService() {

    private val myPolicy = factory.createBuildTrigger(
        RemoteTriggerPolicy(timeService, myCustomTriggersManager),
        Logger.getInstance(CustomTriggerService::class.qualifiedName)
    )

    override fun getName() = "teamcityKotlinTrigger"
    override fun getDisplayName() = "Custom Trigger..."

    override fun describeTrigger(buildTriggerDescriptor: BuildTriggerDescriptor): String {
        val properties = buildTriggerDescriptor.properties
        val triggerPolicyPath = TriggerUtil.getTargetTriggerPolicyPath(properties)
            ?: return "Trigger policy is not selected"

        val policyName = CustomTriggerPolicyDescriptor.policyPathToPolicyName(triggerPolicyPath)

        val projectId = buildTriggerDescriptor.properties["projectId"] ?: return "Project id cannot be determined"
        val project = myProjectManager.findProjectByExternalId(projectId) ?: return "Project cannot be determined"

        val disabledStatus =
            if (myCustomTriggersManager.isTriggerPolicyEnabled(policyName, project)) ""
            else "(disabled)"

        return "Uses $policyName $disabledStatus"
    }

    override fun getTriggerPropertiesProcessor() = PropertiesProcessor { properties: Map<String, String> ->
        val triggerPolicy = TriggerUtil.getTargetTriggerPolicyPath(properties)
        val triggerProperties = TriggerUtil.parseTriggerProperties(properties)

        val errors = mutableListOf<InvalidProperty>()

        fun addInvalidProperty(s1: String, s2: String) = errors.add(InvalidProperty(s1, s2))

        if (triggerPolicy.isNullOrBlank())
            addInvalidProperty(Constants.TRIGGER_POLICY_PATH, "A trigger policy should be specified")

        if (triggerProperties == null)
            addInvalidProperty(Constants.PROPERTIES, "Properties should be 'key=value' pairs on separate lines")

        errors
    }

    override fun getEditParametersUrl() =
        myPluginDescriptor.getPluginResourcesPath(CUSTOM_TRIGGERS_LIST_CONTROLLER)

    override fun getBuildTriggeringPolicy() = myPolicy
    override fun isMultipleTriggersPerBuildTypeAllowed() = true
}
