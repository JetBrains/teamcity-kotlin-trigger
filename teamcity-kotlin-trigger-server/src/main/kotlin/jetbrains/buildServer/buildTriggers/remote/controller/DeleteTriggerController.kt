package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.servlet.ModelAndView
import java.io.File
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal const val DELETE_TRIGGER_REQUEST_PATH = "/admin/deleteCustomTriggerPolicy.html"

internal class DeleteTriggerController(
    myWebControllerManager: WebControllerManager,
    private val myProjectManager: ProjectManager,
    private val myPluginDescriptor: PluginDescriptor,
    private val myCustomTriggersManager: CustomTriggersManager
) : BaseController() {

    private val myLogger = Logger.getInstance(DeleteTriggerController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(DELETE_TRIGGER_REQUEST_PATH, this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val project = myProjectManager.findProjectByRequest(request, myLogger)
            ?: run {
                myLogger.error("Cannot delete trigger policy: the request did not specify any project id")
                return null
            }
        val triggerFileName = request.getParameter("fileName")
            ?: run {
                myLogger.error("Cannot delete trigger policy: the request did not specify any filename")
                return null
            }

        val pluginDataDirectory = project.getPluginDataDirectory(myPluginDescriptor.pluginName)
        val triggerFile = File(pluginDataDirectory, triggerFileName)

        val policyUsages = myCustomTriggersManager.getUsagesInProjectAndSubprojects(triggerFile.absolutePath, project)

        if (policyUsages.isNotEmpty()) {
            myLogger.warn("Failed to delete policy: it has usages (policy path is ${triggerFile.absolutePath})")
            return null
        }

        triggerFile.delete()
        return null
    }
}