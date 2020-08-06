package jetbrains.buildServer.buildTriggers.remote

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.controller.DisableTriggerController
import jetbrains.buildServer.serverSide.BuildTypeIdentity
import jetbrains.buildServer.serverSide.BuildTypeSettings
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.web.openapi.PluginDescriptor
import org.springframework.stereotype.Component
import java.io.File

@Component
class CustomTriggersManager(
    myPluginDescriptor: PluginDescriptor,
    private val myProjectManager: ProjectManager
) {
    private val myLogger = Logger.getInstance(CustomTriggersManager::class.qualifiedName)
    private val myPluginName = myPluginDescriptor.pluginName
    private val myTriggerPolicyUpdated = mutableMapOf<String, Boolean>()

    fun isTriggerPolicyEnabled(triggerPolicyPath: String): Boolean {
        val policyName = CustomTriggerPolicyDescriptor.policyPathToPolicyName(triggerPolicyPath)
        val project = myProjectManager.findProjectByPolicyPath(triggerPolicyPath, "Failed to check if policy is enabled")
            ?: return false

        return DisableTriggerController.isTriggerPolicyEnabled(policyName, project)
    }

    internal fun setTriggerPolicyEnabled(triggerPolicyPath: String, enabled: Boolean) {
        val policyName = CustomTriggerPolicyDescriptor.policyPathToPolicyName(triggerPolicyPath)

        val action = if (enabled) "enable" else "disable"
        val project = myProjectManager.findProjectByPolicyPath(triggerPolicyPath, "Failed to $action the policy")
            ?: return

        DisableTriggerController.setTriggerPolicyEnabled(policyName, project, enabled)
    }

    private fun ProjectManager.findProjectByPolicyPath(triggerPolicyPath: String, failureMessage: String): SProject? {
        val policyDescriptorRegistry =
            rootProject.getCustomDataStorage(CustomTriggerPolicyDescriptor::class.qualifiedName!!)

        val projectExternalId = policyDescriptorRegistry.getValue(triggerPolicyPath)
            ?: run {
                myLogger.warn("$failureMessage: policy '$triggerPolicyPath' is not associated with a project")
                return null
            }
        return myProjectManager.findProjectByExternalId(projectExternalId)
            ?: run {
                myLogger.warn("$failureMessage: no project found by associated project id '$projectExternalId'")
                null
            }
    }

    internal fun isTriggerPolicyUpdated(path: String) = myTriggerPolicyUpdated.computeIfAbsent(path) { true }

    internal fun setTriggerPolicyUpdated(path: String, updated: Boolean) {
        myTriggerPolicyUpdated[path] = updated
    }

    fun localCustomTriggers(project: SProject) = localCustomTriggerFiles(project)
        .map {
            createCustomTriggerPolicyDescriptor(it, project)
        }

    private fun localCustomTriggerFiles(project: SProject) = project.getPluginDataDirectory(myPluginName)
        .apply { mkdirs() }
        .listFiles()
        ?.asList().orEmpty()

    // TODO: clean up
    private fun createCustomTriggerPolicyDescriptor(file: File, project: SProject): CustomTriggerPolicyDescriptor {
        val policyDescriptorRegistryFeature = myProjectManager.rootProject
            .getAvailableFeaturesOfType(CustomTriggerPolicyDescriptor::class.qualifiedName!!)
            .firstOrNull { it.parameters["id"] == file.absolutePath }

        if (policyDescriptorRegistryFeature == null) {
            val params = mapOf(
                "id" to file.absolutePath,
                "projectExternalId" to project.externalId
            )
            myProjectManager.rootProject
                .addFeature(CustomTriggerPolicyDescriptor::class.qualifiedName!!, params)
        } else {
            val params = policyDescriptorRegistryFeature.parameters.toMutableMap()
            params["projectExternalId"] = project.externalId
            myProjectManager.rootProject
                .updateFeature(policyDescriptorRegistryFeature.id, policyDescriptorRegistryFeature.type, params)
        }

        return CustomTriggerPolicyDescriptor(file, project)
    }

    fun inheritedCustomTriggerFiles(project: SProject) = project.projectPath.dropLast(1)
        .flatMap(this::localCustomTriggers)

    fun allUsableCustomTriggerFiles(project: SProject) = project.projectPath
        .flatMap(this::localCustomTriggers)

    fun getUsages(triggerPolicyPath: String, project: SProject): List<BuildTypeIdentity> {
        val usages = mutableListOf<BuildTypeIdentity>()

        project.buildTypeTemplates
            .filter { it.hasUsagesOf(triggerPolicyPath) }
            .forEach { usages.add(it) }

        project.buildTypes
            .filter { it.hasUsagesOf(triggerPolicyPath) }
            .forEach { usages.add(it) }

        return usages
    }

    private fun BuildTypeSettings.hasUsagesOf(triggerPolicyPath: String) = buildTriggersCollection
        .filter { it.buildTriggerService is CustomTriggerService }
        .any { it.properties[Constants.TRIGGER_POLICY_PATH] == triggerPolicyPath }
}
