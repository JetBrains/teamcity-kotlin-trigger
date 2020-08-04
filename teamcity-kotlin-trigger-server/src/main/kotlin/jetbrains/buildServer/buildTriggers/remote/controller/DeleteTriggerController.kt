package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.Constants
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.BuildTypeSettings
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal class DeleteTriggerController(myWebControllerManager: WebControllerManager): BaseController() {
    private val myLogger = Logger.getInstance(DeleteTriggerController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(REQUEST_PATH, this)
    }

    companion object {
        const val REQUEST_PATH = "/admin/deleteCustomTriggerPolicy.html"
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        println("DELETE WITH PARAMETERS:")
        request.parameterMap.forEach { println(it.key + " " + it.value.joinToString { it }) }
        println()
        return null
    }

    private fun Collection<BuildTypeSettings>.removeTriggersOfPolicyPath(triggerPolicyPath: String) {
        forEach { settings ->
            settings.buildTriggersCollection.filter {
                it.properties[Constants.TRIGGER_POLICY_PATH] == triggerPolicyPath
            }.forEach {
                settings.removeBuildTrigger(it)
            }
        }
    }
}