package jetbrains.buildServer.buildTriggers.remote.controller

import jetbrains.buildServer.buildTriggers.remote.CustomTriggerPolicyDescriptor
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.auth.Permission
import jetbrains.buildServer.serverSide.auth.SecurityContext
import jetbrains.buildServer.web.openapi.PluginDescriptor

// TODO: single responsibility
class CustomTriggersManager(
    myPluginDescriptor: PluginDescriptor,
    private val mySecurityContext: SecurityContext
) {
    private val myPluginName = myPluginDescriptor.pluginName
    private val myTriggerPolicyUpdated = mutableMapOf<String, Boolean>()

    fun isTriggerPolicyUpdated(path: String) = myTriggerPolicyUpdated.computeIfAbsent(path) { true }

    fun setTriggerPolicyUpdated(path: String, updated: Boolean) {
        myTriggerPolicyUpdated[path] = updated
    }

    fun localCustomTriggers(project: SProject) = localCustomTriggerFiles(project).map {
        CustomTriggerPolicyDescriptor(it, project)
    }

    fun inheritedCustomTriggerFiles(project: SProject) = project.projectPath.dropLast(1)
        .flatMap(this::localCustomTriggers)

    fun allUsableCustomTriggerFiles(project: SProject) = project.projectPath
        .flatMap(this::localCustomTriggers)

    fun canEditProject(project: SProject) = mySecurityContext.authorityHolder
        .isPermissionGrantedForProject(project.projectId, Permission.EDIT_PROJECT)

    fun canEditProjects(projects: Collection<SProject>) = projects.all(this::canEditProject)

    private fun localCustomTriggerFiles(project: SProject) = project.getPluginDataDirectory(myPluginName).apply {
        mkdirs()
    }.listFiles()?.asList().orEmpty()
}