package jetbrains.buildServer.buildTriggers.remote.controller

import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.web.openapi.PluginDescriptor
import java.io.File

class CustomTriggersBean(
    myPluginDescriptor: PluginDescriptor,
    myProject: SProject
) {
    private val myPluginName = myPluginDescriptor.pluginName
    private val myProjectPath = myProject.projectPath

    val projectId = myProject.projectId
    val pluginDataDirectory = myProject.getPluginDataDirectory(myPluginName).apply { mkdirs() }

    fun getFiles(): List<File> = myProjectPath.flatMap { it.listFiles() }

    fun getLocalFileNames(): List<String> = myProjectPath.last().listFiles().map(File::getName)

    fun getParentsFileNames(): List<String> = myProjectPath.dropLast(1).flatMap {
        it.listFiles().map { it.name }
    }

    private fun SProject.listFiles() = getPluginDataDirectory(myPluginName).apply {
        mkdirs()
    }.listFiles()?.asList().orEmpty()
}