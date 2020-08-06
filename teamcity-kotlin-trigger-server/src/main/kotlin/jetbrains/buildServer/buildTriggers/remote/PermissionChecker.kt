package jetbrains.buildServer.buildTriggers.remote

import jetbrains.buildServer.serverSide.BuildTypeIdentity
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.auth.Permission
import jetbrains.buildServer.serverSide.auth.SecurityContext
import org.springframework.stereotype.Component

@Component
class PermissionChecker(private val mySecurityContext: SecurityContext) {

    fun canEditProject(project: SProject) = mySecurityContext.authorityHolder
        .isPermissionGrantedForProject(project.projectId, Permission.EDIT_PROJECT)

    fun canEditBuildTypeIdentities(identities: Collection<BuildTypeIdentity>) = identities.all {
        canEditProject(it.project)
    }
}