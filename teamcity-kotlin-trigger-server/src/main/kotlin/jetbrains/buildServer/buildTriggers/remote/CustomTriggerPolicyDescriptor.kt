package jetbrains.buildServer.buildTriggers.remote

import jetbrains.buildServer.serverSide.SProject
import java.io.File

class CustomTriggerPolicyDescriptor internal constructor(file: File, val project: SProject) {
    val filePath = file.absolutePath // unique on the whole server
    val policyName = policyPathToPolicyName(filePath) // unique in current project's scope
    val fileName = file.name

    companion object {
        fun policyPathToPolicyName(policyPath: String) = File(policyPath).nameWithoutExtension
    }
}