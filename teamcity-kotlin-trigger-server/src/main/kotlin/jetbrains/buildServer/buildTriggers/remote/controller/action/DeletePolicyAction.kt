package jetbrains.buildServer.buildTriggers.remote.controller.action

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.CustomTriggerPolicyDescriptor
import jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager
import jetbrains.buildServer.buildTriggers.remote.controller.PolicyAction
import jetbrains.buildServer.buildTriggers.remote.findProjectByRequest
import jetbrains.buildServer.serverSide.ProjectManager
import java.io.File
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class DeletePolicyAction(
    private val myProjectManager: ProjectManager,
    private val myCustomTriggersManager: CustomTriggersManager
) : PolicyAction(
    ACTION,
    ERROR_KEY
) {
    private val myLogger = Logger.getInstance(DeletePolicyAction::class.qualifiedName)

    companion object {
        const val ACTION = "deletePolicy"
        const val ERROR_KEY = "deletePolicyError"
    }

    override fun canProcess(request: HttpServletRequest) = super.canProcess(request) &&
            myProjectManager.findProjectByRequest(request, myLogger) != null &&
            request.getParameter("policyName") != null

    override fun processPost(request: HttpServletRequest, response: HttpServletResponse) {
        val project = myProjectManager.findProjectByRequest(request, myLogger) ?: return
        val policyName = request.getParameter("policyName") ?: return
        val policyDescriptor = CustomTriggerPolicyDescriptor(policyName, project)

        val policyPath = myCustomTriggersManager.deleteCustomTriggerPolicy(policyDescriptor)
            ?: throw PolicyActionException("Failed to delete policy '$policyName', it has usages")

        File(policyPath).delete()
        return
    }
}