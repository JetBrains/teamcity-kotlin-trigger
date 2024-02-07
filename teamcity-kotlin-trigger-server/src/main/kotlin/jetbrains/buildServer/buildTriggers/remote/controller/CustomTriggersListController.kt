

package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager
import jetbrains.buildServer.buildTriggers.remote.findProjectByRequest
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.stereotype.Controller
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal const val CUSTOM_TRIGGERS_LIST_CONTROLLER = "customTriggersListController.html"

/** Provides the trigger policy selection screen with data needed to obtain all currently visible policies */
@Controller
class CustomTriggersListController(
    private val myProjectManager: ProjectManager,
    private val myPluginDescriptor: PluginDescriptor,
    private val myCustomTriggersManager: CustomTriggersManager,
    myWebControllerManager: WebControllerManager
) : BaseController() {

    private val myLogger = Logger.getInstance(CustomTriggersListController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(
            myPluginDescriptor.getPluginResourcesPath(CUSTOM_TRIGGERS_LIST_CONTROLLER),
            this
        )
    }

    override fun doHandle(req: HttpServletRequest, res: HttpServletResponse): ModelAndView? {
        val mv = ModelAndView(myPluginDescriptor.getPluginResourcesPath("customTriggerPolicyProperties.jsp"))
        val project = myProjectManager.findProjectByRequest(req, myLogger)
            ?: throw RuntimeException("The request did not specify any project id")

        mv.model["customTriggersManager"] = myCustomTriggersManager
        mv.model["project"] = project

        return mv
    }
}