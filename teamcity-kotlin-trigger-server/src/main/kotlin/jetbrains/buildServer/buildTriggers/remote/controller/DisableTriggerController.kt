package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal const val DISABLE_TRIGGER_REQUEST_PATH = "/admin/disableCustomTriggerPolicy.html"

internal class DisableTriggerController(
    myWebControllerManager: WebControllerManager,
    private val myProjectManager: ProjectManager
) : BaseController() {

    private val myLogger = Logger.getInstance(DisableTriggerController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(DISABLE_TRIGGER_REQUEST_PATH, this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val project = myProjectManager.findProjectByRequest(request, myLogger)
            ?: run {
                myLogger.error("Cannot disable trigger policy: the request did not specify any project id")
                return null
            }
        val triggerPolicyName = request.getParameter("triggerPolicyName")
            ?: run {
                myLogger.error("Cannot disable trigger policy: the request did not specify any policy name")
                return null
            }

        val enable = request.getParameter("enable")?.toBoolean() ?: false
        val triggerPolicyEnabled = isTriggerPolicyEnabled(triggerPolicyName, project)

        if (triggerPolicyEnabled == enable) {
            val enabledStatus = if (triggerPolicyEnabled) "enabled" else "disabled"
            myLogger.debug("Trigger policy '$triggerPolicyName' already $enabledStatus")
            return null
        }

        setTriggerPolicyEnabled(triggerPolicyName, project, enable)
        return null
    }

    companion object {
        fun isTriggerPolicyEnabled(triggerPolicyName: String, project: SProject): Boolean =
            project
                .getCustomDataStorage(DisableTriggerController::class.qualifiedName!!)
                .getValue(commonId(triggerPolicyName, project))
                ?.toBoolean()
                ?: true

        fun setTriggerPolicyEnabled(triggerPolicyName: String, project: SProject, enabled: Boolean) {
            project
                .getCustomDataStorage(DisableTriggerController::class.qualifiedName!!)
                .putValue(commonId(triggerPolicyName, project), enabled.toString())
        }

        private fun commonId(triggerPolicyName: String, project: SProject) =
            "${project.externalId}_${triggerPolicyName}"
    }
}