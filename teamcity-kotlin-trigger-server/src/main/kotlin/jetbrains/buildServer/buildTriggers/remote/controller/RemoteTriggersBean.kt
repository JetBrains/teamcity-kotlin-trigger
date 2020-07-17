package jetbrains.buildServer.buildTriggers.remote.controller

import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.web.openapi.PluginDescriptor

class RemoteTriggersBean(
    myProjectManager: ProjectManager,
    myPluginDescriptor: PluginDescriptor
) {
    private val dataDirectory = myProjectManager.rootProject.getPluginDataDirectory(myPluginDescriptor.pluginName)

    fun getFileNames(): List<String> {
//        val dataDirectory = myProjectManager.rootProject.getPluginDataDirectory(myPluginDescriptor.pluginName)
        return dataDirectory.list()?.asList() ?: listOf("Hi???")
    }

    fun getFullPathTo(fileName: String): String {
        return "${dataDirectory.absolutePath}${System.getProperty("file.separator")}$fileName"
    }
}