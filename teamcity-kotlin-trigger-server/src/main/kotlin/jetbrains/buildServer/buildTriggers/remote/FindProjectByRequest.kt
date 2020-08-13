package jetbrains.buildServer.buildTriggers.remote

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SProject
import javax.servlet.http.HttpServletRequest

private const val BT_PREFIX = "buildType:"
private const val TEMPLATE_PREFIX = "template:"

fun ProjectManager.findProjectByRequest(request: HttpServletRequest, logger: Logger): SProject? {
    val projectId = request.getParameter("projectId")

    if (projectId != null) {
        val project = findProjectByExternalId(projectId)

        if (project != null) return project
        else logger.warn("No project found by project id '$projectId'")
    }

    val id = request.getParameter("id") ?: return null

    return when {
        id.startsWith(BT_PREFIX) -> {
            val buildTypeId = id.substring(BT_PREFIX.length)
            val buildType = findBuildTypeByExternalId(buildTypeId)

            buildType?.project
                ?: run {
                    logger.warn("No build type found by id '$buildTypeId'")
                    null
                }
        }
        id.startsWith(TEMPLATE_PREFIX) -> {
            val templateId = id.substring(TEMPLATE_PREFIX.length)
            val template = findBuildTypeTemplateByExternalId(templateId)

            template?.project
                ?: run {
                    logger.warn("No template found by id '$templateId'")
                    null
                }
        }
        else -> {
            val buildType = findBuildTypeByExternalId(id)
            val template = findBuildTypeTemplateByExternalId(id)

            buildType?.project
                ?: template?.project
                ?: run {
                    logger.warn("Cannot obtain current project: id '$id' does not belong to neither project, build type, nor template")
                    null
                }
        }
    }
}