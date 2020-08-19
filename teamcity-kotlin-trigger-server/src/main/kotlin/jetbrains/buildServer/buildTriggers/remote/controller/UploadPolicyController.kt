package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.CustomTriggerPolicy
import jetbrains.buildServer.buildTriggers.remote.CustomTriggerPolicyDescriptor
import jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager
import jetbrains.buildServer.buildTriggers.remote.findProjectByRequest
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
import java.net.URLClassLoader
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
internal class UploadPolicyController(
    private val myProjectManager: ProjectManager,
    private val myPluginDescriptor: PluginDescriptor,
    private val myCustomTriggersManager: CustomTriggersManager,
    myWebControllerManager: WebControllerManager
) : MultipartFormController() {

    private val myLogger = Logger.getInstance(UploadPolicyController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(PATH, this)
    }

    override fun doPost(request: HttpServletRequest, response: HttpServletResponse): ModelAndView {
        val modelAndView = ModelAndView(myPluginDescriptor.getPluginResourcesPath("_fileUploadResponse.jsp"))
        val model = modelAndView.model
        model["jsBase"] = "BS.UploadTriggerDialog"

        val updatedFileName = request.getParameter("updatedFileName")

        try {
            val triggerJarName = request.getParameter("fileName")
                ?: throw UploadException("Cannot upload trigger policy: the request did not specify any filename")

            val project = myProjectManager.findProjectByRequest(request, myLogger)
                ?: throw UploadException("Cannot upload trigger policy: the request did not specify any project id")

            val validatedMultipart = validateMultipartFileName(request, triggerJarName)
            val policyName = triggerJarName.substringBeforeLast('.')

            val localPolicyExists = project.hasLocalPolicy(triggerJarName)
            val descendant = project.projects.firstOrNull { it.hasLocalPolicy(triggerJarName) }
            val ancestor = project.projectPath.dropLast(1)
                .firstOrNull { it.hasLocalPolicy(triggerJarName) }

            val updateMode = updatedFileName != null && updatedFileName.isNotBlank()

            if (updateMode && updatedFileName != triggerJarName)
                throw UploadException("Uploaded jar's filename doesn't match the updated file's name")

            if (!updateMode && localPolicyExists)
                throw UploadException("File is already loaded to this project")

            if (ancestor != null)
                throw UploadException("File is already loaded to this project's ancestor '${ancestor.fullName}'")

            if (descendant != null)
                throw UploadException("File is already loaded to this project's descendant '${descendant.fullName}'")

            val policyDescriptor = myCustomTriggersManager.createCustomTriggerPolicy(policyName, project)

            val triggerFile = File(policyDescriptor.filePath)
            triggerFile.mkdirs()
            validatedMultipart.transferTo(triggerFile)

            try {
                validatePolicyFile(triggerFile)
            } catch (e: Throwable) {
                triggerFile.delete()
                myCustomTriggersManager.deleteCustomTriggerPolicy(policyName, project)
                throw e
            }

            myCustomTriggersManager.setTriggerPolicyUpdated(policyName, project, true)

            if (!updateMode || !localPolicyExists)
                myCustomTriggersManager.setTriggerPolicyEnabled(policyName, project, true)
        } catch (e: Exception) {
            myLogger.warnAndDebugDetails("Failed to upload a trigger policy", e)
            model["error"] = e.message
            return modelAndView
        }

        return modelAndView
    }

    private fun SProject.hasLocalPolicy(fileName: String) =
        myCustomTriggersManager.localCustomTriggers(this).any { it.fileName == fileName }

    private fun validateMultipartFileName(
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
    
    private fun validatePolicyFile(policyFile: File) {
        val parentClassLoader = CustomTriggerPolicy::class.java.classLoader
        val classLoader = URLClassLoader(arrayOf(policyFile.toURI().toURL()), parentClassLoader)

        val policyName = CustomTriggerPolicyDescriptor.policyPathToPolicyName(policyFile.absolutePath)
        val policyClassName = CustomTriggerPolicyDescriptor.policyClassQualifiedName(policyName)

        val policyClass = try {
            Class.forName(policyClassName, false, classLoader)
        } catch (e: ClassNotFoundException) {
            throw UploadException("Malformed policy archive: cannot find $policyClassName class")
        }

        if (!CustomTriggerPolicy::class.java.isAssignableFrom(policyClass))
            throw UploadException("Malformed policy archive: policy class is not an implementation of the ${CustomTriggerPolicy::class.qualifiedName} interface")
    }

    private class UploadException(msg: String) : RuntimeException(msg)

    companion object {
        const val PATH = "/admin/uploadCustomTriggerPolicy.html"
    }
}
