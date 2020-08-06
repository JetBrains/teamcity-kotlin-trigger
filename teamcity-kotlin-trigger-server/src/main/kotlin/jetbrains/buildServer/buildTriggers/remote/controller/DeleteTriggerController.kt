package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.stereotype.Controller
import org.springframework.web.servlet.ModelAndView
import java.io.File
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
internal class DeleteTriggerController(
    myWebControllerManager: WebControllerManager,
    private val myProjectManager: ProjectManager,
    private val myPluginDescriptor: PluginDescriptor,
    private val myCustomTriggersManager: CustomTriggersManager
) : BaseController() {

    private val myLogger = Logger.getInstance(DeleteTriggerController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(PATH, this)
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

        val policyPath = myPluginDescriptor.getPluginResourcesPath(triggerFileName)
        val triggerFile = File(policyPath)

        val policyUsages = myCustomTriggersManager.getUsages(policyPath, project)

        if (policyUsages.isNotEmpty()) {
            myLogger.warn("Failed to delete policy: it has usages (policy path is $policyPath)")
            return null
        }

        triggerFile.delete()
        return null
    }

    companion object {
        const val PATH = "/admin/deleteCustomTriggerPolicy.html"
    }
}