package jetbrains.buildServer.buildTriggers.remote.controller

import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.web.openapi.PluginDescriptor
import java.nio.file.Path

class RemoteTriggersBean(
    myProjectManager: ProjectManager,
    myPluginDescriptor: PluginDescriptor
) {
    private val myDataDirectory = myProjectManager.rootProject.getPluginDataDirectory(myPluginDescriptor.pluginName)

    fun getFileNames(): List<String> = myDataDirectory.list()?.asList().orEmpty()

    fun getFullPathTo(fileName: String): Path =
        myDataDirectory.toPath().resolve(fileName)
}