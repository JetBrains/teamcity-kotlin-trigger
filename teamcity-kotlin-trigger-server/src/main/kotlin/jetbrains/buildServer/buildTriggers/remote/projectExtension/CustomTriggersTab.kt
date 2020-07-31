package jetbrains.buildServer.buildTriggers.remote.projectExtension

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.controller.CustomTriggersBean
import jetbrains.buildServer.buildTriggers.remote.controller.CustomTriggersController
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.web.openapi.*
import javax.servlet.http.HttpServletRequest

class CustomTriggersTab(
    pagePlaces: PagePlaces,
    private val myPluginDescriptor: PluginDescriptor,
    private val myProjectManager: ProjectManager
) :
    SimpleCustomTab(
        pagePlaces,
        PlaceId.EDIT_PROJECT_PAGE_TAB,
        "customTriggers",
        myPluginDescriptor.getPluginResourcesPath("customTriggersTab.jsp"),
        "Custom Triggers"
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
                val bean = CustomTriggersBean(myPluginDescriptor, project)
                model["customTriggersBean"] = bean
                model["resourcesPath"] = myPluginDescriptor.pluginResourcesPath
            }
        }
    }
}