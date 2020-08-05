package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal const val CUSTOM_TRIGGER_PROPERTIES_CONTROLLER = "customTriggerPropertiesController.html"

private const val BT_PREFIX = "buildType:"
private const val TEMPLATE_PREFIX = "template:"

/** Provides the trigger policy selection screen with data needed to obtain all currently visible policies */
class CustomTriggerPropertiesController(
    private val myProjectManager: ProjectManager,
    private val myPluginDescriptor: PluginDescriptor,
    private val myCustomTriggersManager: CustomTriggersManager,
    myWebControllerManager: WebControllerManager
) : BaseController() {

    private val myLogger = Logger.getInstance(CustomTriggerPropertiesController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(
            myPluginDescriptor.getPluginResourcesPath(CUSTOM_TRIGGER_PROPERTIES_CONTROLLER),
            this
        )
    }

    override fun doHandle(req: HttpServletRequest, res: HttpServletResponse): ModelAndView? {
        val mv = ModelAndView(myPluginDescriptor.getPluginResourcesPath("customTriggerPolicyProperties.jsp"))
        val project = myProjectManager.findProjectByRequest(req, myLogger)
            ?: run {
                myLogger.warn("The request did not specify any project id, will use root project")
                myProjectManager.rootProject
            }

        mv.model["customTriggersManager"] = myCustomTriggersManager
        mv.model["project"] = project
        return mv
    }
}

fun ProjectManager.findProjectByRequest(request: HttpServletRequest, logger: Logger): SProject? {
    logger.debug("Request parameters are: " +
            request.parameterMap.entries.joinToString { (k, v) ->
                "'$k': " + v.joinToString(prefix = "[", postfix = "]") { it }
            })

    val projectId = request.getParameter("projectId")

    if (projectId != null) {
        val project = findProjectById(projectId) ?: findProjectByExternalId(projectId)

        if (project != null) return project
        else logger.warn("No project found by project id '$projectId'")
    }

    val id = request.getParameter("id")

    return when {
        id.startsWith(BT_PREFIX) -> {
            val buildTypeId = id.substring(BT_PREFIX.length)
            val buildType = findBuildTypeById(buildTypeId) ?: findBuildTypeByExternalId(buildTypeId)

            buildType?.project
                ?: run {
                    logger.warn("No build type found by id '$buildTypeId'")
                    null
                }
        }
        id.startsWith(TEMPLATE_PREFIX) -> {
            val templateId = id.substring(TEMPLATE_PREFIX.length)
            val template = findBuildTypeTemplateById(templateId) ?: findBuildTypeTemplateByExternalId(templateId)

            template?.project
                ?: run {
                    logger.warn("No template found by id '$templateId'")
                    null
                }
        }
        else -> {
            val buildType = findBuildTypeById(id) ?: findBuildTypeByExternalId(id)
            val template = findBuildTypeTemplateById(id) ?: findBuildTypeTemplateByExternalId(id)

            buildType?.project
                ?: template?.project
                ?: run {
                    logger.warn("Cannot obtain current project: id '$id' does not belong to neither project, build type, nor template")
                    null
                }
        }
    }
}