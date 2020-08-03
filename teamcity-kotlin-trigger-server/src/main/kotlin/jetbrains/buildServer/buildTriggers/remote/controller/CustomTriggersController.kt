package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private const val btPrefix = "buildType:"
private const val templatePrefix = "template:"

class CustomTriggersController(
    private val myProjectManager: ProjectManager,
    private val myPluginDescriptor: PluginDescriptor,
    private val myCustomTriggersBean: CustomTriggersManager,
    myWebControllerManager: WebControllerManager
) : BaseController() {
    private val myLogger = Logger.getInstance(CustomTriggersController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(
            myPluginDescriptor.getPluginResourcesPath("customTriggerController.html"),
            this
        )
    }

    override fun doHandle(req: HttpServletRequest, res: HttpServletResponse): ModelAndView? {
        val mv = ModelAndView(myPluginDescriptor.getPluginResourcesPath("teamcity-kotlin-trigger.jsp"))
        val project = req.findProject(myProjectManager, myLogger) ?: myProjectManager.rootProject

        mv.model["customTriggersBean"] = myCustomTriggersBean
        mv.model["project"] = project
        return mv
    }

    internal companion object {
        fun HttpServletRequest.findProject(
            projectManager: ProjectManager,
            logger: Logger
        ): SProject? {
            val projectId = getParameter("projectId")

            if (projectId != null) {
                val project = projectManager.findProjectById(projectId)
                    ?: projectManager.findProjectByExternalId(projectId)

                if (project != null) {
                    return project
                } else {
                    logger.error("No project found by project id '$projectId'")
                }
            }

            val id = getParameter("id")
            return when {
                id.startsWith(btPrefix) -> {
                    val buildTypeId = id.substring(btPrefix.length)
                    val buildType =
                        projectManager.findBuildTypeById(buildTypeId)
                            ?: projectManager.findBuildTypeByExternalId(buildTypeId)

                    buildType?.project ?: run {
                        logger.error("No build type found by id '$buildTypeId'")
                        null
                    }
                }
                id.startsWith(templatePrefix) -> {
                    val templateId = id.substring(templatePrefix.length)
                    val template = projectManager.findBuildTypeTemplateById(templateId)
                        ?: projectManager.findBuildTypeTemplateByExternalId(templateId)

                    template?.project ?: run {
                        logger.error("No template found by id '$templateId'")
                        null
                    }
                }
                else -> {
                    logger.error("Cannot obtain current project: id '$id' does not belong to neither build type nor template")
                    null
                }
            }
        }
    }
}