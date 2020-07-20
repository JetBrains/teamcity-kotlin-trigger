package jetbrains.buildServer.buildTriggers.remote.controller

import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.web.openapi.PluginDescriptor

class RemoteTriggersBean(
    myProjectManager: ProjectManager,
    myPluginDescriptor: PluginDescriptor
) {
    private val dataDirectory = myProjectManager.rootProject.getPluginDataDirectory(myPluginDescriptor.pluginName)

    fun getFileNames(): List<String> = dataDirectory.list()?.asList().orEmpty()

    fun getFullPathTo(fileName: String): String =
        "${dataDirectory.absolutePath}${System.getProperty("file.separator")}$fileName"
}