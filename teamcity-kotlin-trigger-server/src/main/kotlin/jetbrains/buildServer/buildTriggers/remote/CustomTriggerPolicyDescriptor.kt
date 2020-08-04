package jetbrains.buildServer.buildTriggers.remote

import jetbrains.buildServer.serverSide.BuildTypeIdentity
import jetbrains.buildServer.serverSide.BuildTypeSettings
import jetbrains.buildServer.serverSide.SProject
import java.io.File

// TODO: experiment with visibility
class CustomTriggerPolicyDescriptor internal constructor(file: File, val project: SProject) {
    val policyName = file.nameWithoutExtension // unique in current project's scope
    val fileName = file.name
    val filePath = file.absolutePath // unique on the whole server

    fun getUsagesInProjectAndSubprojects(project: SProject): List<BuildTypeIdentity> {
        val usages = mutableListOf<BuildTypeIdentity>()

        project.buildTypeTemplates
            .filter { it.hasUsages() }
            .forEach { usages.add(it) }

        project.buildTypes
            .filter { it.hasUsages() }
            .forEach { usages.add(it) }

        return usages
    }

    private fun BuildTypeSettings.hasUsages() = buildTriggersCollection
        .filter { it.buildTriggerService is CustomTriggerService }
        .any { it.properties[Constants.TRIGGER_POLICY_PATH] == filePath }
}