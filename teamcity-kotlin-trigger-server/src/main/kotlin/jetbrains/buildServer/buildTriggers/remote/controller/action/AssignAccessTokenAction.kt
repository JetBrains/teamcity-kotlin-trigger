

package jetbrains.buildServer.buildTriggers.remote.controller.action

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.CustomTriggerPolicyDescriptor
import jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager
import jetbrains.buildServer.buildTriggers.remote.controller.PolicyAction
import jetbrains.buildServer.buildTriggers.remote.findProjectByRequest
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.crypt.RSACipher
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AssignAccessTokenAction(
    private val myProjectManager: ProjectManager,
    private val myCustomTriggersManager: CustomTriggersManager
) : PolicyAction(
    ACTION,
    ERROR_KEY
) {
    private val myLogger = Logger.getInstance(AssignAccessTokenAction::class.qualifiedName)

    companion object {
        const val ACTION = "assignAccessToken"
        const val ERROR_KEY = "assignAccessTokenError"
    }

    override fun canProcess(request: HttpServletRequest) = super.canProcess(request) &&
            request.getParameter("policyName")?.isNotBlank() == true &&
            myProjectManager.findProjectByRequest(request, myLogger) != null &&
            request.getParameter("encryptedToken") != null

    override fun processPost(request: HttpServletRequest, response: HttpServletResponse) {
        val project = myProjectManager.findProjectByRequest(request, myLogger) ?: return
        val policyName = request.getParameter("policyName") ?: return
        val policyDescriptor = CustomTriggerPolicyDescriptor(policyName, project)

        val encryptedToken = request.getParameter("encryptedToken")
        val token = RSACipher.decryptWebRequestData(encryptedToken)

        if (token?.isNotBlank() != true)
            throw PolicyActionException("Access token cannot be empty")

        myCustomTriggersManager.setTriggerPolicyAuthToken(policyDescriptor, token)
    }
}