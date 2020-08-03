package jetbrains.buildServer.buildTriggers.remote

import jetbrains.buildServer.serverSide.BuildTypeSettings
import jetbrains.buildServer.serverSide.SProject
import java.io.File

// TODO: experiment with visibility
class CustomTriggerPolicyDescriptor(file: File, val project: SProject) {
    val fileName = file.name // unique in current project's scope
    val filePath = file.absolutePath // unique in the whole system

    fun getUsagesInProjectAndSubprojects(project: SProject): List<SProject> {
        val result = mutableListOf(project)
        result.addAll(project.projects)

        return result.filter { it.hasUsagesInProject() }
    }

    private fun SProject.hasUsagesInProject(): Boolean =
        ownBuildTypes.any { it.hasUsages() }
                || ownBuildTypeTemplates.any { it.hasUsages() }


    private fun BuildTypeSettings.hasUsages() = buildTriggersCollection
        .filter { it.buildTriggerService is CustomTriggerService }
        .any { it.properties[Constants.TRIGGER_POLICY_PATH] == filePath }
}