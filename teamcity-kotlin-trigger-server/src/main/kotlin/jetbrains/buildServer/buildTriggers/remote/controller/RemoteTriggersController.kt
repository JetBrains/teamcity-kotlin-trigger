package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class RemoteTriggersController(
    private val myProjectManager: ProjectManager,
    private val myPluginDescriptor: PluginDescriptor,
    myWebControllerManager: WebControllerManager
) : BaseController() {
    private val myLogger = Logger.getInstance(RemoteTriggersController::class.qualifiedName)

    private val btPrefix = "buildType:"
    private val templatePrefix = "template:"

    init {
        myWebControllerManager.registerController(
            myPluginDescriptor.getPluginResourcesPath("remoteTriggerController.html"),
            this
        )
    }

    override fun doHandle(req: HttpServletRequest, res: HttpServletResponse): ModelAndView? {
        val mv = ModelAndView(myPluginDescriptor.getPluginResourcesPath("teamcity-kotlin-trigger.jsp"))
        val id = req.getParameter("id")

        val project = when {
            id.startsWith(btPrefix) -> {
                val buildTypeId = id.substring(btPrefix.length)
                val buildType = myProjectManager.findBuildTypeByExternalId(buildTypeId)
                buildType?.project ?: run {
                    myLogger.error("No project found by build type id '$buildTypeId', will use root project")
                    myProjectManager.rootProject
                }
            }
            id.startsWith(templatePrefix) -> {
                val templateId = id.substring(templatePrefix.length)
                val template = myProjectManager.findBuildTypeTemplateByExternalId(templateId)
                template?.project ?: run {
                    myLogger.error("No project found by build type template id '$templateId', will use root project")
                    myProjectManager.rootProject
                }
            }
            else -> {
                myLogger.error("Cannot obtain current project: id '$id' does not belong to neither build type nor template, will use root project")
                myProjectManager.rootProject
            }
        }

        val bean = RemoteTriggersBean(myPluginDescriptor, project)
        mv.model["remoteTriggersBean"] = bean
        return mv
    }
}