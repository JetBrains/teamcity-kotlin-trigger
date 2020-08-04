package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.servlet.ModelAndView
import java.io.File
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal class DeleteTriggerController(
    myWebControllerManager: WebControllerManager,
    private val myProjectManager: ProjectManager,
    private val myPluginDescriptor: PluginDescriptor
) : BaseController() {
    private val myLogger = Logger.getInstance(DeleteTriggerController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(REQUEST_PATH, this)
    }

    companion object {
        const val REQUEST_PATH = "/admin/deleteCustomTriggerPolicy.html"
    }

    // TODO: add logging
    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val project = CustomTriggersController.run {
            request.findProject(myProjectManager, myLogger)
        } ?: return null

        val triggerJarName = request.getParameter("fileName") ?: return null
        val pluginDataDirectory = project.getPluginDataDirectory(myPluginDescriptor.pluginName)

        File(pluginDataDirectory, triggerJarName).delete()
        return null
    }
}