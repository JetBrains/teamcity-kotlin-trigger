package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal class DisableTriggerController(
    myWebControllerManager: WebControllerManager,
    private val myProjectManager: ProjectManager
) : BaseController() {
    private val myLogger = Logger.getInstance(DisableTriggerController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(REQUEST_PATH, this)
    }

    companion object {
        const val REQUEST_PATH = "/admin/disableCustomTriggerPolicy.html"

        fun isTriggerPolicyEnabled(triggerPolicyPath: String, project: SProject): Boolean =
            project.getCustomDataStorage(DisableTriggerController::class.qualifiedName!!)
                .getValue(triggerPolicyPath)
                ?.toBoolean()
                ?: true
    }

    // TODO: add logging
    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val project = CustomTriggersController.run {
            request.findProject(myProjectManager, myLogger)
        } ?: return null

        val triggerPolicyPath = request.getParameter("triggerPolicyPath") ?: return null
        val enable = request.getParameter("enable")?.toBoolean() ?: false

        val triggerPolicyEnabled = isTriggerPolicyEnabled(triggerPolicyPath, project)
        if (triggerPolicyEnabled == enable)
            return null

        val customDataStorage = project.getCustomDataStorage(DisableTriggerController::class.qualifiedName!!)
        customDataStorage.putValue(triggerPolicyPath, enable.toString())

        return null
    }
}