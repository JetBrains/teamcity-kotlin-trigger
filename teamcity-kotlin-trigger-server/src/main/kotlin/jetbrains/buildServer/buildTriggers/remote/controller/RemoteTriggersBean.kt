package jetbrains.buildServer.buildTriggers.remote.controller

import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.web.openapi.PluginDescriptor
import java.io.File

class RemoteTriggersBean(
    myProjectManager: ProjectManager,
    myPluginDescriptor: PluginDescriptor
) {
    private val myDataDirectory = myProjectManager.rootProject.getPluginDataDirectory(myPluginDescriptor.pluginName)

    fun getFileNames(): List<String> = myDataDirectory.list()?.asList().orEmpty()

    fun getFullPathTo(fileName: String): String =
        "${myDataDirectory.absolutePath}${File.separator}$fileName"
}