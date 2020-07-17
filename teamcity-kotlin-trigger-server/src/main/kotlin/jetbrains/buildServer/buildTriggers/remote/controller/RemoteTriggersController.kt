package jetbrains.buildServer.buildTriggers.remote.controller

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

    init {
        myWebControllerManager.registerController(
            myPluginDescriptor.getPluginResourcesPath("remoteTriggerController.html"),
            this
        )
    }

    override fun doHandle(req: HttpServletRequest, res: HttpServletResponse): ModelAndView? {
        val bean = RemoteTriggersBean(myProjectManager, myPluginDescriptor)
        val mv = ModelAndView(myPluginDescriptor.getPluginResourcesPath("teamcity-kotlin-trigger.jsp"))
        mv.model["remoteTriggersBean"] = bean
        return mv
    }
}