

package jetbrains.buildServer.buildTriggers.remote

import jetbrains.buildServer.serverSide.SProject

/** Represents a trigger policy descriptor in a scope of some project */
data class CustomTriggerPolicyDescriptor(
    val policyName: String, // unique in current project's scope (i.e. the project itself and all its descendants)
    val project: SProject // some project where the policy is visible
)