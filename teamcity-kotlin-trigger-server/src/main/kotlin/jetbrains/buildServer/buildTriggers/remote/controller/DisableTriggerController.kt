package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor
import jetbrains.buildServer.buildTriggers.remote.Constants
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.BuildTypeIdentity
import jetbrains.buildServer.serverSide.BuildTypeSettings
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

        if (enable) {
            val disabledIds = customDataStorage.values
                ?.filterNot { it.value?.toBoolean() ?: true } // filter out enabled triggers
                ?.keys
                ?: emptySet()
            project.buildTypeTemplates.enableTriggersOfPolicyPath(triggerPolicyPath, disabledIds)
            project.buildTypes.enableTriggersOfPolicyPath(triggerPolicyPath, disabledIds)

            disabledIds.forEach {
                customDataStorage.putValue(it, "true") // mark as enabled
            }
            customDataStorage.putValue(triggerPolicyPath, "true")
        } else {
            val disabledFromTemplates = project.buildTypeTemplates.disableTriggersOfPolicyPath(triggerPolicyPath)
            val disabledFromBuildTypes = project.buildTypes.disableTriggersOfPolicyPath(triggerPolicyPath)

            disabledFromTemplates.forEach { customDataStorage.putValue(it, "false") }
            disabledFromBuildTypes.forEach { customDataStorage.putValue(it, "false") }

            customDataStorage.putValue(triggerPolicyPath, "false")
        }

        return null
    }


    private fun <T> Collection<T>.disableTriggersOfPolicyPath(triggerPolicyPath: String): Collection<String>
            where T : BuildTypeSettings,
                  T : BuildTypeIdentity = flatMap { settings ->
        settings.buildTriggersCollection.filter {
            it.properties[Constants.TRIGGER_POLICY_PATH] == triggerPolicyPath &&
                    settings.isEnabled(it.id)
        }.map {
            settings.setEnabled(it.id, false)
            commonId(settings, it)
        }
    }

    private fun <T> Collection<T>.enableTriggersOfPolicyPath(triggerPolicyPath: String, disabledIds: Collection<String>)
            where T : BuildTypeSettings,
                  T : BuildTypeIdentity = forEach { settings ->
        settings.buildTriggersCollection.filter {
            it.properties[Constants.TRIGGER_POLICY_PATH] == triggerPolicyPath && commonId(settings, it) in disabledIds
        }.forEach {
            settings.setEnabled(it.id, true)
        }
    }

    private fun commonId(buildTypeIdentity: BuildTypeIdentity, buildTriggerDescriptor: BuildTriggerDescriptor) =
        buildTypeIdentity.externalId + "_" + buildTriggerDescriptor.id
}