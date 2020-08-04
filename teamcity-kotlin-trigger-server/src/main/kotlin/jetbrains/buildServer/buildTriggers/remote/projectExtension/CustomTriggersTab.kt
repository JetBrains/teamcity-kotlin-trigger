package jetbrains.buildServer.buildTriggers.remote.projectExtension

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.controller.CustomTriggersController
import jetbrains.buildServer.buildTriggers.remote.controller.CustomTriggersManager
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.web.openapi.*
import javax.servlet.http.HttpServletRequest

class CustomTriggersTab(
    pagePlaces: PagePlaces,
    myPluginDescriptor: PluginDescriptor,
    private val myProjectManager: ProjectManager,
    private val myCustomTriggersManager: CustomTriggersManager
) :
    SimpleCustomTab(
        pagePlaces,
        PlaceId.EDIT_PROJECT_PAGE_TAB,
        "customTriggerPolicies",
        myPluginDescriptor.getPluginResourcesPath("customTriggerPolicies.jsp"),
        "Custom Trigger Policies"
    ) {
    private val myLogger = Logger.getInstance(CustomTriggersTab::class.qualifiedName)

    init {
        setPosition(PositionConstraint.last())
        register()
    }

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest) {
        super.fillModel(model, request)
        CustomTriggersController.run {
            val project = request.findProject(myProjectManager, myLogger)
            if (project != null) {
                model["customTriggersManager"] = myCustomTriggersManager
                model["project"] = project
            }
        }
    }
}