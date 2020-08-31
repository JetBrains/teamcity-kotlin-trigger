package jetbrains.buildServer.buildTriggers.remote.controller.action

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.CustomTriggerPolicyDescriptor
import jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager
import jetbrains.buildServer.buildTriggers.remote.controller.PolicyAction
import jetbrains.buildServer.buildTriggers.remote.findProjectByRequest
import jetbrains.buildServer.serverSide.ProjectManager
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class DisablePolicyAction(
    private val myProjectManager: ProjectManager,
    private val myCustomTriggersManager: CustomTriggersManager
) : PolicyAction(
    ACTION,
    ERROR_KEY
) {

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
        val policyName = request.getParameter("triggerPolicyName") ?: return
        val policyDescriptor = CustomTriggerPolicyDescriptor(policyName, project)


        val enable = request.getParameter("enable")?.toBoolean() ?: false
        val triggerPolicyEnabled = myCustomTriggersManager.isTriggerPolicyEnabled(policyDescriptor)

        if (triggerPolicyEnabled == enable) {
            val enabledStatus = if (triggerPolicyEnabled) "enabled" else "disabled"
            myLogger.debug("Trigger policy '$policyName' already $enabledStatus")
            return
        }

        myCustomTriggersManager.setTriggerPolicyEnabled(policyDescriptor, enable)
        return
    }
}