package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.controllers.MultipartFormController
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.ModelAndView
import java.io.File
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class UploadTriggerController(
    private val myProjectManager: ProjectManager,
    private val myPluginDescriptor: PluginDescriptor,
    private val myCustomTriggersBean: CustomTriggersManager,
    myWebControllerManager: WebControllerManager
) : MultipartFormController() {
    private val myLogger = Logger.getInstance(UploadTriggerController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(
            "/admin/uploadCustomTriggerPolicy.html",
            this
        )
    }

    override fun doPost(request: HttpServletRequest, response: HttpServletResponse): ModelAndView {
        val modelAndView = ModelAndView(myPluginDescriptor.getPluginResourcesPath("_fileUploadResponse.jsp"))
        val model = modelAndView.model
        model["jsBase"] = "BS.UploadTriggerDialog"

        val project = CustomTriggersController.run {
            request.findProject(myProjectManager, myLogger)
        } ?: return modelAndView // FIXME: maybe error?

        val triggerJarName = request.getParameter("fileName")
        val updatedFileName = request.getParameter("updatedFileName")

        try {
            if (updatedFileName.isNullOrBlank()) {
                if (project.hasLocalFile(triggerJarName, myCustomTriggersBean))
                    throw UploadException("File already exists")
            } else if (updatedFileName != triggerJarName)
                throw UploadException("Uploaded jar's filename doesn't match the updated file's name")

            val ancestor = project.projectPath.dropLast(1)
                .firstOrNull { it.hasLocalFile(triggerJarName, myCustomTriggersBean) }
            if (ancestor != null)
                throw UploadException("File is already loaded to this project's ancestor '${ancestor.fullName}'")

            val descendant = project.ownProjects.firstOrNull { it.hasLocalFile(triggerJarName, myCustomTriggersBean) }
            if (descendant != null)
                throw UploadException("File is already loaded to this project's descendant '${descendant.fullName}'")

            val pluginDataDirectory = project.getPluginDataDirectory(myPluginDescriptor.pluginName)
            val triggerFile = File(pluginDataDirectory, triggerJarName)

            validateMultipart(request, triggerJarName).transferTo(triggerFile)
            myCustomTriggersBean.setTriggerPolicyUpdated(triggerFile.absolutePath, true)
        } catch (e: Exception) {
            myLogger.warnAndDebugDetails("Error while uploading a trigger policy", e)
            model["error"] = e.message
            return modelAndView
        }

        return modelAndView
    }

    private fun SProject.hasLocalFile(fileName: String, ctBean: CustomTriggersManager) =
        ctBean.localCustomTriggers(this).any { it.fileName == fileName }

    private fun validateMultipart(
        request: HttpServletRequest,
        triggerJarName: String
    ): MultipartFile {
        val triggerJar = getMultipartFileOrFail(request, "file:fileToUpload")

        if (triggerJar == null || triggerJar.isEmpty)
            throw UploadException("The file is not selected")

        if (StringUtil.isEmpty(triggerJarName))
            throw UploadException("The file name is not specified")

        if (!triggerJarName.endsWith(".jar"))
            throw UploadException("Selected file must be a JAR archive")

        return triggerJar
    }

    private class UploadException(msg: String) : RuntimeException(msg)
}
