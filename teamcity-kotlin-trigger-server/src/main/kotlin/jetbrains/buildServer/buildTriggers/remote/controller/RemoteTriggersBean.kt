package jetbrains.buildServer.buildTriggers.remote.controller

import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.web.openapi.PluginDescriptor
import java.io.File

class RemoteTriggersBean(
    myPluginDescriptor: PluginDescriptor,
    myProject: SProject
) {
    private val myPluginName = myPluginDescriptor.pluginName
    private val myProjectPath = myProject.projectPath

    fun getFiles(): List<File> = myProjectPath.flatMap {
        it.getPluginDataDirectory(myPluginName).apply {
            mkdirs()
        }.listFiles()?.asList().orEmpty()
    }
}