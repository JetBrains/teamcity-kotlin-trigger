package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager
import jetbrains.buildServer.buildTriggers.remote.findProjectByRequest
import jetbrains.buildServer.serverSide.ProjectManager
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class DisablePolicyAction(
    private val myProjectManager: ProjectManager,
    private val myCustomTriggersManager: CustomTriggersManager
) : PolicyAction(ACTION, ERROR_KEY) {

    private val myLogger = Logger.getInstance(DisablePolicyAction::class.qualifiedName)

    companion object {
        const val ACTION = "disablePolicy"
        const val ERROR_KEY = "disablePolicyError"
    }

    override fun canProcess(request: HttpServletRequest) = super.canProcess(request) &&
            request.getParameter("triggerPolicyName") != null &&
            myProjectManager.findProjectByRequest(request, myLogger) != null

    override fun processPost(request: HttpServletRequest, response: HttpServletResponse) {
        val project = myProjectManager.findProjectByRequest(request, myLogger) ?: return
        val triggerPolicyName = request.getParameter("triggerPolicyName") ?: return

        val enable = request.getParameter("enable")?.toBoolean() ?: false
        val triggerPolicyEnabled = myCustomTriggersManager.isTriggerPolicyEnabled(triggerPolicyName, project)

        if (triggerPolicyEnabled == enable) {
            val enabledStatus = if (triggerPolicyEnabled) "enabled" else "disabled"
            myLogger.debug("Trigger policy '$triggerPolicyName' already $enabledStatus")
            return
        }

        myCustomTriggersManager.setTriggerPolicyEnabled(triggerPolicyName, project, enable)
        return
    }
}