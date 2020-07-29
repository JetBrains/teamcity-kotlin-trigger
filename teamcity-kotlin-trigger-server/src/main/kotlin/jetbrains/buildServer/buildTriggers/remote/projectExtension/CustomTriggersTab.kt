package jetbrains.buildServer.buildTriggers.remote.projectExtension

import jetbrains.buildServer.web.openapi.*

class CustomTriggersTab(pagePlaces: PagePlaces, pluginDescriptor: PluginDescriptor) :
    SimpleCustomTab(
        pagePlaces,
        PlaceId.EDIT_PROJECT_PAGE_TAB,
        "customTriggers",
        pluginDescriptor.getPluginResourcesPath("customTriggersTab.jsp"),
        "Custom Triggers"
    ) {
    init {
        setPosition(PositionConstraint.last())
        register()
    }
}