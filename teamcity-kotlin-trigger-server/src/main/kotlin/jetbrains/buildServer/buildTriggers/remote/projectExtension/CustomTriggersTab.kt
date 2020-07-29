package jetbrains.buildServer.buildTriggers.remote.projectExtension

import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.web.openapi.PagePlaces
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.project.ProjectTab
import javax.servlet.http.HttpServletRequest

class CustomTriggersTab(pagePlaces: PagePlaces, projectManager: ProjectManager, pluginDescriptor: PluginDescriptor) :
    ProjectTab(
        "customTriggersTab",
        "Custom triggers",
        pagePlaces,
        projectManager,
        pluginDescriptor.getPluginResourcesPath("customTriggersTab.jsp")
    ) {
    override fun fillModel(model: MutableMap<String, Any>, req: HttpServletRequest, project: SProject, user: SUser?) {
        // FIXME: nothing yet
    }


}