package jetbrains.buildServer.buildTriggers.remote

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.serverSide.crypt.EncryptUtil
import jetbrains.buildServer.web.openapi.PluginDescriptor
import org.springframework.stereotype.Component

private val myFeaturePrefix = CustomTriggersManager::class.qualifiedName!! + "_"

@Component
class CustomTriggersManager(
    myPluginDescriptor: PluginDescriptor,
    private val myProjectManager: ProjectManager,
    private val myPolicyFileManager: TriggerPolicyFileManager<String>
) {
    private val myLogger = Logger.getInstance(CustomTriggersManager::class.qualifiedName)

    private val myPluginName = myPluginDescriptor.pluginName
    private val myTriggerPolicyUpdatedMap = mutableMapOf<String, Boolean>()

    private val myPolicyPathParam = "policyPath"
    private val myPolicyEnabledParam = "enabled"
    private val myPolicyAuthTokenParam = "authToken"

    fun createCustomTriggerPolicy(policyDescriptor: CustomTriggerPolicyDescriptor): String {
        val (policyName, project) = policyDescriptor
        policyDescriptor.getOrCreatePolicyFeature()

        val policyPath = project.getPathOfOwnPolicy(policyName)
        policyDescriptor.updatePolicyFeature(myPolicyPathParam to policyPath)

        return policyPath
    }

    fun deleteCustomTriggerPolicy(policyDescriptor: CustomTriggerPolicyDescriptor): String? {
        val (policyName, project) = policyDescriptor
        if (getUsages(policyDescriptor).isNotEmpty()) {
            myLogger.debug("Policy '$policyName' of project '${project.externalId}' still has usages and cannot be deleted")
            return null
        }

        val policyPath = getTriggerPolicyFilePath(policyDescriptor) ?: run {
            myLogger.debug(policyDescriptor.policyDoesNotExistMessage())
            return null
        }
        policyDescriptor.removePolicyFeature()
        return policyPath
    }

    /** @return the unique path of the trigger policy, or null if the policy does not exist */
    fun getTriggerPolicyFilePath(policyDescriptor: CustomTriggerPolicyDescriptor): String? {
        val policyFeature = policyDescriptor.getPolicyFeature() ?: run {
            myLogger.debug(policyDescriptor.policyDoesNotExistMessage())
            return null
        }

        return policyFeature.parameters[myPolicyPathParam]
            ?: throw IllegalStateException("Each policy must have a path assigned")
    }

    fun getTriggerPolicyAuthToken(policyDescriptor: CustomTriggerPolicyDescriptor): String? {
        val tokenFeature = policyDescriptor.getPolicyFeature() ?: return null
        val token = tokenFeature.parameters[myPolicyAuthTokenParam]
        if (token.isNullOrEmpty()) return null

        return EncryptUtil.unscramble(token)
    }

    fun setTriggerPolicyAuthToken(policyDescriptor: CustomTriggerPolicyDescriptor, newToken: String) {
        val token =
            if (newToken.isBlank()) ""
            else EncryptUtil.scramble(newToken)

        policyDescriptor.updatePolicyFeature(myPolicyAuthTokenParam to token)
    }

    fun deleteTriggerPolicyAuthToken(policyDescriptor: CustomTriggerPolicyDescriptor) =
        setTriggerPolicyAuthToken(policyDescriptor, "")

    fun isTriggerPolicyEnabled(policyDescriptor: CustomTriggerPolicyDescriptor): Boolean {
        val policyFeature = policyDescriptor.getPolicyFeature() ?: run {
            myLogger.debug(policyDescriptor.policyDoesNotExistMessage())
            return false
        }

        return policyFeature
            .parameters[myPolicyEnabledParam]
            ?.toBoolean()
            ?: true
    }

    fun setTriggerPolicyEnabled(policyDescriptor: CustomTriggerPolicyDescriptor, enabled: Boolean) {
        policyDescriptor.updatePolicyFeature(myPolicyEnabledParam to enabled.toString())
    }

    fun isTriggerPolicyUpdated(policyDescriptor: CustomTriggerPolicyDescriptor): Boolean {
        val policyPath = getTriggerPolicyFilePath(policyDescriptor) ?: run {
            myLogger.debug(policyDescriptor.policyDoesNotExistMessage())
            return false
        }
        return myTriggerPolicyUpdatedMap.computeIfAbsent(policyPath) { true }
    }

    fun setTriggerPolicyUpdated(policyDescriptor: CustomTriggerPolicyDescriptor, updated: Boolean) {
        val policyPath = getTriggerPolicyFilePath(policyDescriptor) ?: run {
            myLogger.debug(policyDescriptor.policyDoesNotExistMessage())
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

    fun getUsages(policyDescriptor: CustomTriggerPolicyDescriptor): List<BuildTypeIdentity> {
        val (policyName, project) = policyDescriptor

        val filteredBuildTypes = project.buildTypes.asSequence()
            .filter { it.hasUsagesOf(policyName) }
            .asSequence<BuildTypeIdentity>()

        val filteredTemplates = project.buildTypeTemplates.asSequence()
            .filter { it.hasUsagesOf(policyName) }

        return (filteredBuildTypes + filteredTemplates).toList()
    }

    private fun Collection<SProjectFeatureDescriptor>.toPolicyDescriptors() =
        filter { it.isPolicyFeature }
            .mapNotNull { feature ->
                myProjectManager.findProjectById(feature.projectId)
                    ?.to(feature)
            }
            .map { (definingProject, feature) ->
                CustomTriggerPolicyDescriptor(feature.policyName, definingProject)
            }

    private fun SProject.getPathOfOwnPolicy(policyName: String): String =
        getPluginDataDirectory(myPluginName)
            .resolve(myPolicyFileManager.createPolicyFileName(policyName))
            .absolutePath

    private fun CustomTriggerPolicyDescriptor.policyDoesNotExistMessage() =
        "Policy '$policyName' does not exist in project '${project.externalId}' or its ancestors"
}

private val SProjectFeatureDescriptor.isPolicyFeature: Boolean get() = type.startsWith(myFeaturePrefix)
private val SProjectFeatureDescriptor.policyName: String get() = type.substring(myFeaturePrefix.length)

private fun CustomTriggerPolicyDescriptor.getOrCreatePolicyFeature() =
    getPolicyFeature()
        ?: project
            .addFeature(myFeaturePrefix + policyName, emptyMap())
            .also { project.persist() }

private fun CustomTriggerPolicyDescriptor.getPolicyFeature(): SProjectFeatureDescriptor? =
    project.getAvailableFeaturesOfType(myFeaturePrefix + policyName).firstOrNull()

private fun CustomTriggerPolicyDescriptor.updatePolicyFeature(vararg entries: Pair<String, String>) {
    val feature = getPolicyFeature() ?: return

    val params = feature.parameters.toMutableMap()
    params.putAll(entries)
    project.updateFeature(feature.id, feature.type, params)
    project.persist()
}

private fun CustomTriggerPolicyDescriptor.removePolicyFeature() {
    val feature = getPolicyFeature() ?: return
    project.removeFeature(feature.id)
    project.persist()
}

private fun BuildTypeSettings.hasUsagesOf(triggerPolicyName: String) = buildTriggersCollection.asSequence()
    .filter { it.buildTriggerService is CustomTriggerService }
    .any { TriggerUtil.getTargetTriggerPolicyName(it.properties) == triggerPolicyName }
