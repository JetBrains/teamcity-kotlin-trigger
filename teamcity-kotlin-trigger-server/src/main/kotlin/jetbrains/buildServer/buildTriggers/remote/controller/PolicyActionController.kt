package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager
import jetbrains.buildServer.buildTriggers.remote.controller.action.AssignAccessTokenAction
import jetbrains.buildServer.buildTriggers.remote.controller.action.DeletePolicyAction
import jetbrains.buildServer.buildTriggers.remote.controller.action.DisablePolicyAction
import jetbrains.buildServer.buildTriggers.remote.controller.action.RemoveAccessTokenAction
import jetbrains.buildServer.controllers.ActionErrors
import jetbrains.buildServer.controllers.BaseAjaxActionController
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.web.openapi.ControllerAction
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.jdom.Element
import org.springframework.stereotype.Controller
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
class PolicyActionController(
    myWebControllerManager: WebControllerManager,
    myProjectManager: ProjectManager,
    myCustomTriggersManager: CustomTriggersManager
) : BaseAjaxActionController(myWebControllerManager) {

    companion object {
        const val PATH = "/admin/customTriggerPolicyAction.html"
    }

    init {
        registerAction(DisablePolicyAction(myProjectManager, myCustomTriggersManager))
        registerAction(DeletePolicyAction(myProjectManager, myCustomTriggersManager))

        registerAction(AssignAccessTokenAction(myProjectManager, myCustomTriggersManager))
        registerAction(RemoveAccessTokenAction(myProjectManager, myCustomTriggersManager))

        myWebControllerManager.registerController(PATH, this)
    }
}

abstract class PolicyAction(private val actionName: String, private val errorKey: String) : ControllerAction {
    private val myLogger = Logger.getInstance(PolicyAction::class.qualifiedName)

    companion object {
        const val ACTION_KEY = "policyAction"
    }

    override fun canProcess(request: HttpServletRequest) =
        request.method.toLowerCase() == "post" && request.getParameter(ACTION_KEY) == actionName

    final override fun process(request: HttpServletRequest, response: HttpServletResponse, ajaxResponse: Element?) {
        val errors = ActionErrors()
        try {
            processPost(request, response)
        } catch (e: PolicyActionException) {
            myLogger.warnAndDebugDetails("Failed to process '$actionName' policy action", e)
            errors.addError(errorKey, e.message)
            if (ajaxResponse != null)
                errors.serialize(ajaxResponse)
        }
    }

    @Throws(PolicyActionException::class)
    abstract fun processPost(request: HttpServletRequest, response: HttpServletResponse)

    protected class PolicyActionException(msg: String) : RuntimeException(msg)
}