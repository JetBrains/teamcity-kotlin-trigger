package jetbrains.buildServer.buildTriggers.remote.controller.action

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager
import jetbrains.buildServer.buildTriggers.remote.controller.PolicyAction
import jetbrains.buildServer.buildTriggers.remote.findProjectByRequest
import jetbrains.buildServer.serverSide.ProjectManager
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class RemoveAccessTokenAction(
    private val myProjectManager: ProjectManager,
    private val myCustomTriggersManager: CustomTriggersManager
) : PolicyAction(
    ACTION,
    ERROR_KEY
) {
    private val myLogger = Logger.getInstance(RemoveAccessTokenAction::class.qualifiedName)

    companion object {
        const val ACTION = "removeAccessToken"
        const val ERROR_KEY = "removeAccessTokenError"
    }

    override fun canProcess(request: HttpServletRequest) = super.canProcess(request) &&
            request.getParameter("policyName") != null &&
            myProjectManager.findProjectByRequest(request, myLogger) != null

    override fun processPost(request: HttpServletRequest, response: HttpServletResponse) {
        val project = myProjectManager.findProjectByRequest(request, myLogger) ?: return
        val policyName = request.getParameter("policyName") ?: return

        myCustomTriggersManager.deleteTriggerPolicyAuthToken(policyName, project)
    }
}
