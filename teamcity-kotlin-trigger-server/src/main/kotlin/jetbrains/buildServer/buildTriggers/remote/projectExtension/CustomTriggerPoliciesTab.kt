package jetbrains.buildServer.buildTriggers.remote.projectExtension

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager
import jetbrains.buildServer.buildTriggers.remote.PermissionChecker
import jetbrains.buildServer.buildTriggers.remote.controller.findProjectByRequest
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.web.openapi.*
import javax.servlet.http.HttpServletRequest

class CustomTriggerPoliciesTab(
    pagePlaces: PagePlaces,
    private val myPluginDescriptor: PluginDescriptor,
    private val myProjectManager: ProjectManager,
    private val myCustomTriggersManager: CustomTriggersManager,
    private val myPermissionChecker: PermissionChecker
) : SimpleCustomTab(
    pagePlaces,
    PlaceId.EDIT_PROJECT_PAGE_TAB,
    "customTriggerPolicies",
    myPluginDescriptor.getPluginResourcesPath("customTriggerPoliciesTab.jsp"),
    "Custom Trigger Policies"
) {
    private val myLogger = Logger.getInstance(CustomTriggerPoliciesTab::class.qualifiedName)

    init {
        setPosition(PositionConstraint.last())
        register()
    }

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest) {
        model["customTriggersManager"] = myCustomTriggersManager
        model["permissionChecker"] = myPermissionChecker
        model["pluginDescriptor"] = myPluginDescriptor

        val project = myProjectManager.findProjectByRequest(request, myLogger)
            ?: run {
                myLogger.error("Failed to provide the extension tab with an instance of a project")
                return
            }

        model["project"] = project
    }
}