package jetbrains.buildServer.buildTriggers.remote

import jetbrains.buildServer.serverSide.SProject
import java.io.File

/** Represents a file-based trigger policy descriptor for custom triggers */
interface CustomTriggerPolicyDescriptor {
    val fileName: String
    val filePath: String // unique on the whole server
    val policyName: String // unique in current project's scope (i.e. the project itself and all its descendants)

    val project: SProject // the project where the policy is defined

    companion object {
        fun policyPathToPolicyName(policyPath: String) = File(policyPath).nameWithoutExtension
        fun policyNameToFileName(policyName: String) = "$policyName.jar"
        fun policyClassQualifiedName(policyName: String) = "jetbrains.buildServer.buildTriggers.remote.compiled.$policyName"
    }
}
