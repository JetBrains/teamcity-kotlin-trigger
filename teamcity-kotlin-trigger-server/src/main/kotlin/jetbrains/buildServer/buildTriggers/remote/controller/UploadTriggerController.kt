package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager
import jetbrains.buildServer.controllers.MultipartFormController
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.stereotype.Controller
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.ModelAndView
import java.io.File
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
internal class UploadTriggerController(
    private val myProjectManager: ProjectManager,
    private val myPluginDescriptor: PluginDescriptor,
    private val myCustomTriggersManager: CustomTriggersManager,
    myWebControllerManager: WebControllerManager
) : MultipartFormController() {

    private val myLogger = Logger.getInstance(UploadTriggerController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(PATH, this)
    }

    override fun doPost(request: HttpServletRequest, response: HttpServletResponse): ModelAndView {
        val modelAndView = ModelAndView(myPluginDescriptor.getPluginResourcesPath("_fileUploadResponse.jsp"))
        val model = modelAndView.model
        model["jsBase"] = "BS.UploadTriggerDialog"

        val updatedFileName = request.getParameter("updatedFileName")
        val triggerJarName = request.getParameter("fileName")
            ?: run {
                myLogger.error("Cannot upload trigger policy: the request did not specify any filename")
                return modelAndView
            }
        val project = myProjectManager.findProjectByRequest(request, myLogger)
            ?: run {
                myLogger.error("Cannot upload trigger policy: the request did not specify any project id")
                return modelAndView
            }

        try {
            val updateMode = updatedFileName != null && updatedFileName.isNotBlank()
            if (updateMode) {
                if (updatedFileName != triggerJarName)
                    throw UploadException("Uploaded jar's filename doesn't match the updated file's name")
            } else {
                if (project.hasLocalFile(triggerJarName))
                    throw UploadException("File already exists")
            }

            val ancestor = project.projectPath.dropLast(1)
                .firstOrNull { it.hasLocalFile(triggerJarName) }
            if (ancestor != null)
                throw UploadException("File is already loaded to this project's ancestor '${ancestor.fullName}'")

            val descendant = project.projects.firstOrNull { it.hasLocalFile(triggerJarName) }
            if (descendant != null)
                throw UploadException("File is already loaded to this project's descendant '${descendant.fullName}'")

            val policyPath = myPluginDescriptor.getPluginResourcesPath(triggerJarName)
            val triggerFile = File(policyPath)

            validateMultipart(request, triggerJarName).transferTo(triggerFile)
            myCustomTriggersManager.setTriggerPolicyUpdated(policyPath, true)

            if (!updateMode)
                myCustomTriggersManager.setTriggerPolicyEnabled(policyPath, true)
        } catch (e: Exception) {
            myLogger.warnAndDebugDetails("Failed to upload a trigger policy", e)
            model["error"] = e.message
            return modelAndView
        }

        return modelAndView
    }

    private fun SProject.hasLocalFile(fileName: String) =
        myCustomTriggersManager.localCustomTriggers(this).any { it.fileName == fileName }

    private fun validateMultipart(
        request: HttpServletRequest,
        triggerJarName: String
    ): MultipartFile {
        val triggerJar = getMultipartFileOrFail(request, "file:fileToUpload")

        return when {
            triggerJar == null || triggerJar.isEmpty ->
                throw UploadException("The file is not selected")

            StringUtil.isEmpty(triggerJarName) ->
                throw UploadException("The file name is not specified")

            !triggerJarName.endsWith(".jar") ->
                throw UploadException("Selected file must be a JAR archive")

            else -> triggerJar
        }
    }

    private class UploadException(msg: String) : RuntimeException(msg)

    companion object {
        const val PATH = "/admin/uploadCustomTriggerPolicy.html"
    }
}
