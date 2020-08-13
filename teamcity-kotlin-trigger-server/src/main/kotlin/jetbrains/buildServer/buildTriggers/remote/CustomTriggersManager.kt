package jetbrains.buildServer.buildTriggers.remote

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.web.openapi.PluginDescriptor
import org.springframework.stereotype.Component
import java.io.File

private val myFeaturePrefix = CustomTriggersManager::class.qualifiedName!! + "_"

@Component
class CustomTriggersManager(myPluginDescriptor: PluginDescriptor, private val myProjectManager: ProjectManager) {
    private val myLogger = Logger.getInstance(CustomTriggersManager::class.qualifiedName)

    private val myPluginName = myPluginDescriptor.pluginName
    private val myTriggerPolicyUpdatedMap = mutableMapOf<String, Boolean>()
    private val myPolicyEnabledParam = "enabled"

    fun createCustomTriggerPolicy(policyName: String, project: SProject): CustomTriggerPolicyDescriptor {
        val policyPath = project.getPathOfOwnPolicy(policyName)
        val triggerPolicyDescriptor = CustomTriggerPolicyDescriptorImpl(File(policyPath), project)

        project.getPolicyFeature(policyName) ?: project.createPolicyFeature(policyName)

        return triggerPolicyDescriptor
    }

    fun deleteCustomTriggerPolicy(policyName: String, project: SProject): String? {
        val policyPath = project.getPathOfPolicy(policyName) ?: run {
            myLogger.debug(project.policyDoesNotExistMessage(policyName))
            return null
        }

        if (getUsages(policyPath, project).isNotEmpty()) {
            myLogger.debug("Policy '$policyName' of project '${project.externalId}' still has usages and cannot be deleted")
            return null
        }

        project.removePolicyFeature(policyName)
        return policyPath
    }

    fun isTriggerPolicyEnabled(policyName: String, project: SProject): Boolean {
        val definingProject = project.getAncestorDefiningPolicy(policyName)
        val policyFeature = definingProject?.getPolicyFeature(policyName) ?: run {
            myLogger.debug(project.policyDoesNotExistMessage(policyName))
            return false
        }

        return policyFeature
            .parameters[myPolicyEnabledParam]
            ?.toBoolean()
            ?: true
    }

    fun setTriggerPolicyEnabled(policyName: String, project: SProject, enabled: Boolean) {
        val definingProject = project.getAncestorDefiningPolicy(policyName) ?: run {
            myLogger.debug(project.policyDoesNotExistMessage(policyName))
            return
        }
        definingProject.updateOrCreatePolicyFeature(policyName, myPolicyEnabledParam to enabled.toString())
    }

    fun isTriggerPolicyUpdated(policyName: String, project: SProject): Boolean {
        val policyPath = project.getPathOfPolicy(policyName) ?: run {
            myLogger.debug(project.policyDoesNotExistMessage(policyName))
            return false
        }
        return myTriggerPolicyUpdatedMap.computeIfAbsent(policyPath) { true }
    }

    fun setTriggerPolicyUpdated(policyName: String, project: SProject, updated: Boolean) {
        val policyPath = project.getPathOfPolicy(policyName) ?: run {
            myLogger.debug(project.policyDoesNotExistMessage(policyName))
            return
        }
        myTriggerPolicyUpdatedMap[policyPath] = updated
    }

    fun localCustomTriggers(project: SProject): Collection<CustomTriggerPolicyDescriptor> =
        project.ownFeatures.toPolicyDescriptors()

    fun allUsableCustomTriggers(project: SProject): Collection<CustomTriggerPolicyDescriptor> =
        project.availableFeatures.toPolicyDescriptors()

    fun inheritedCustomTriggers(project: SProject): Collection<CustomTriggerPolicyDescriptor> =
        project.parentProject
            ?.let { allUsableCustomTriggers(it) }
            ?: emptyList()

    fun getUsages(triggerPolicyPath: String, project: SProject): List<BuildTypeIdentity> {
        val filteredBuildTypes = project.buildTypes.asSequence()
            .filter { it.hasUsagesOf(triggerPolicyPath) }
            .asSequence<BuildTypeIdentity>()

        val filteredTemplates = project.buildTypeTemplates.asSequence()
            .filter { it.hasUsagesOf(triggerPolicyPath) }

        return (filteredBuildTypes + filteredTemplates).toList()
    }

    private fun Collection<SProjectFeatureDescriptor>.toPolicyDescriptors() =
        filter { it.isPolicyFeature }
            .mapNotNull { feature ->
                myProjectManager.findProjectById(feature.projectId)
                    ?.to(feature)
            }
            .map { (definingProject, feature) ->
                val policyPath = definingProject.getPathOfOwnPolicy(feature.policyName)
                CustomTriggerPolicyDescriptorImpl(File(policyPath), definingProject)
            }

    private fun SProject.getAncestorDefiningPolicy(policyName: String): SProject? {
        val projectInternalId = getPolicyFeature(policyName)?.projectId ?: return null
        return myProjectManager.findProjectById(projectInternalId)
    }

    private fun SProject.getPathOfOwnPolicy(policyName: String): String =
        getPluginDataDirectory(myPluginName)
            .resolve(CustomTriggerPolicyDescriptor.policyNameToFileName(policyName))
            .absolutePath

    private fun SProject.getPathOfPolicy(policyName: String): String? =
        getAncestorDefiningPolicy(policyName)?.getPathOfOwnPolicy(policyName)

    private fun SProject.policyDoesNotExistMessage(policyName: String) =
        "Policy '$policyName' does not exist in project '${externalId}' or its ancestors"
}

private val SProjectFeatureDescriptor.isPolicyFeature: Boolean get() = type.startsWith(myFeaturePrefix)
private val SProjectFeatureDescriptor.policyName: String get() = type.substring(myFeaturePrefix.length)

private fun SProject.createPolicyFeature(policyName: String) = addFeature(myFeaturePrefix + policyName, emptyMap())
    .also { persist() }

private fun SProject.getPolicyFeature(policyName: String): SProjectFeatureDescriptor? =
    getAvailableFeaturesOfType(myFeaturePrefix + policyName).firstOrNull()

private fun SProject.updateOrCreatePolicyFeature(policyName: String, vararg entries: Pair<String, String>) {
    val feature = getPolicyFeature(policyName) ?: return

    val params = feature.parameters.toMutableMap()
    params.putAll(entries)
    updateFeature(feature.id, feature.type, params)
    persist()
}

private fun SProject.removePolicyFeature(policyName: String) {
    val feature = getPolicyFeature(policyName) ?: return
    removeFeature(feature.id)
    persist()
}

private fun BuildTypeSettings.hasUsagesOf(triggerPolicyPath: String) = buildTriggersCollection.asSequence()
    .filter { it.buildTriggerService is CustomTriggerService }
    .any { it.properties[Constants.TRIGGER_POLICY_PATH] == triggerPolicyPath }

private class CustomTriggerPolicyDescriptorImpl(file: File, override val project: SProject) :
    CustomTriggerPolicyDescriptor {

    override val fileName: String = file.name
    override val filePath: String = file.absolutePath
    override val policyName = CustomTriggerPolicyDescriptor.policyPathToPolicyName(filePath)
}