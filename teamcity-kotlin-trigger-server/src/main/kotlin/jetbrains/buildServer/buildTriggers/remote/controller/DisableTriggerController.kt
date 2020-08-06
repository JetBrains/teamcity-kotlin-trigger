package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.stereotype.Controller
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
internal class DisableTriggerController(
    myWebControllerManager: WebControllerManager,
    private val myProjectManager: ProjectManager
) : BaseController() {

    private val myLogger = Logger.getInstance(DisableTriggerController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(PATH, this)
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

    // TODO: move
    // TODO: clean up
    companion object {
        const val PATH = "/admin/disableCustomTriggerPolicy.html"

        private const val ID_PARAM = "id"
        private const val ENABLED_PARAM = "enabled"

        fun isTriggerPolicyEnabled(triggerPolicyName: String, project: SProject): Boolean {
            val enableFeature = project.getOwnFeaturesOfType(DisableTriggerController::class.qualifiedName!!)
                .firstOrNull { it.parameters[ID_PARAM] == commonId(triggerPolicyName, project) }

            return if (enableFeature == null) true
            else enableFeature.parameters[ENABLED_PARAM]?.toBoolean() ?: false
        }

        fun setTriggerPolicyEnabled(triggerPolicyName: String, project: SProject, enabled: Boolean) {
            val enableFeature = project.getOwnFeaturesOfType(DisableTriggerController::class.qualifiedName!!)
                .firstOrNull { it.parameters[ID_PARAM] == commonId(triggerPolicyName, project) }
            if (enableFeature == null) {
                val params = mapOf(
                    ID_PARAM to commonId(triggerPolicyName, project),
                    ENABLED_PARAM to enabled.toString()
                )
                project.addFeature(DisableTriggerController::class.qualifiedName!!, params)
            } else {
                val params = enableFeature.parameters.toMutableMap()
                params[ENABLED_PARAM] = enabled.toString()
                project.updateFeature(enableFeature.id, enableFeature.type, params)
            }
        }

        private fun commonId(triggerPolicyName: String, project: SProject) =
            "${project.externalId}_${triggerPolicyName}"
    }
}