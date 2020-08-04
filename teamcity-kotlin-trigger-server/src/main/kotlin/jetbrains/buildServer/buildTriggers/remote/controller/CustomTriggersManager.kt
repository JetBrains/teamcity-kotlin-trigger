package jetbrains.buildServer.buildTriggers.remote.controller

import jetbrains.buildServer.buildTriggers.remote.CustomTriggerPolicyDescriptor
import jetbrains.buildServer.serverSide.BuildTypeIdentity
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.auth.Permission
import jetbrains.buildServer.serverSide.auth.SecurityContext
import jetbrains.buildServer.web.openapi.PluginDescriptor
import java.io.File

// TODO: single responsibility
class CustomTriggersManager(
    myPluginDescriptor: PluginDescriptor,
    private val myProjectManager: ProjectManager,
    private val mySecurityContext: SecurityContext
) {
    private val myPluginName = myPluginDescriptor.pluginName
    private val myTriggerPolicyUpdated = mutableMapOf<String, Boolean>()

    private fun createCustomTriggerPolicyDescriptor(file: File, project: SProject): CustomTriggerPolicyDescriptor {
        val policyDescriptorRegistry =
            myProjectManager.rootProject
                .getCustomDataStorage(CustomTriggerPolicyDescriptor::class.qualifiedName!!)

        policyDescriptorRegistry.putValue(file.absolutePath, project.externalId)

        return CustomTriggerPolicyDescriptor(file, project)
    }

    fun isTriggerPolicyUpdated(path: String) = myTriggerPolicyUpdated.computeIfAbsent(path) { true }

    fun isTriggerPolicyEnabled(path: String): Boolean {
        val policyDescriptorRegistry =
            myProjectManager.rootProject
                .getCustomDataStorage(CustomTriggerPolicyDescriptor::class.qualifiedName!!)

        val projectExternalId = policyDescriptorRegistry.getValue(path) ?: return false
        val project = myProjectManager.findProjectByExternalId(projectExternalId) ?: return false

        return DisableTriggerController.isTriggerPolicyEnabled(path, project)
    }

    fun setTriggerPolicyUpdated(path: String, updated: Boolean) {
        myTriggerPolicyUpdated[path] = updated
    }

    fun localCustomTriggers(project: SProject) = localCustomTriggerFiles(project).map {
        createCustomTriggerPolicyDescriptor(it, project)
    }

    fun inheritedCustomTriggerFiles(project: SProject) = project.projectPath.dropLast(1)
        .flatMap(this::localCustomTriggers)

    fun allUsableCustomTriggerFiles(project: SProject) = project.projectPath
        .flatMap(this::localCustomTriggers)

    fun canEditProject(project: SProject) = mySecurityContext.authorityHolder
        .isPermissionGrantedForProject(project.projectId, Permission.EDIT_PROJECT)

    fun canEdit(identity: Collection<BuildTypeIdentity>) = identity.all {
        canEditProject(it.project)
    }

    private fun localCustomTriggerFiles(project: SProject) = project.getPluginDataDirectory(myPluginName).apply {
        mkdirs()
    }.listFiles()?.asList().orEmpty()
}