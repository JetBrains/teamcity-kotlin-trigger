package jetbrains.buildServer.buildTriggers.remote

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor
import jetbrains.buildServer.buildTriggers.BuildTriggerService
import jetbrains.buildServer.buildTriggers.async.AsyncPolledBuildTriggerFactory
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.serverSide.executors.ExecutorServices
import jetbrains.buildServer.util.TimeService
import jetbrains.buildServer.web.openapi.PluginDescriptor

class RemoteTriggerService(
    private val myPluginDescriptor: PluginDescriptor,
    factory: AsyncPolledBuildTriggerFactory,
    timeService: TimeService,
    executorServices: ExecutorServices
) : BuildTriggerService() {

    private val myPolicy = factory.createBuildTrigger(
        RemoteTriggerPolicy(timeService, executorServices.lowPriorityExecutorService),
        Logger.getInstance(RemoteTriggerService::class.qualifiedName)
    )

    override fun getName() = "teamcityKotlinTrigger"
    override fun getDisplayName() = "Remote triggers"

    override fun describeTrigger(buildTriggerDescriptor: BuildTriggerDescriptor): String {
        val properties = buildTriggerDescriptor.properties

        if (!TriggerUtil.getEnable(properties))
            return "Does nothing (edit configurations to activate it)"

        val triggerPath = TriggerUtil.getTargetTriggerPath(properties)
            ?: return "Trigger is not selected"

        return "Uses ${TriggerUtil.getTriggerName(triggerPath)}"
    }

    override fun getTriggerPropertiesProcessor() =
        PropertiesProcessor { properties: Map<String, String> ->
            val enable = TriggerUtil.getEnable(properties)
            val delay = TriggerUtil.getDelay(properties)
            val triggerPolicy = TriggerUtil.getTargetTriggerPath(properties)

            val rv = mutableListOf<InvalidProperty>()

            if (enable && (delay == null || delay <= 0))
                rv.add(InvalidProperty(Constants.DELAY, "Specify a correct delay, please"))
            if (triggerPolicy.isNullOrBlank())
                rv.add(InvalidProperty(Constants.TRIGGER_POLICY, "A trigger policy must be specified"))

            rv
        }

    override fun getEditParametersUrl() = myPluginDescriptor.getPluginResourcesPath("remoteTriggerController.html")

    override fun isMultipleTriggersPerBuildTypeAllowed() = true
    override fun getBuildTriggeringPolicy() = myPolicy
}
