

package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.*
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
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
internal class UploadPolicyController(
    private val myProjectManager: ProjectManager,
    private val myPluginDescriptor: PluginDescriptor,
    private val myCustomTriggersManager: CustomTriggersManager,
    private val myPolicyFileManager: TriggerPolicyFileManager<String>,
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

        val updatedPolicyName = request.getParameter("updatedPolicyName")
        val updatedFileName =
            if (updatedPolicyName?.isNotBlank() == true)
                myPolicyFileManager.createPolicyFileName(updatedPolicyName)
            else ""

        try {
            val triggerJarName = request.getParameter("fileName")
                ?: throw UploadException("Cannot upload trigger policy: the request did not specify any filename")

            val project = myProjectManager.findProjectByRequest(request, myLogger)
                ?: throw UploadException("Cannot upload trigger policy: the request did not specify any project id")

            val validatedMultipart = validateMultipartFileName(request, triggerJarName)

            val policyName = myPolicyFileManager.getPolicyName(Paths.get(triggerJarName))
            val policyDescriptor = CustomTriggerPolicyDescriptor(policyName, project)

            validateUsages(project, policyName, updatedFileName, triggerJarName)

            var createdPolicy = false
            val policyPath = myCustomTriggersManager.getTriggerPolicyFilePath(policyDescriptor) ?: run {
                createdPolicy = true
                myCustomTriggersManager.createCustomTriggerPolicy(policyDescriptor)
            }

            val fileName = myPolicyFileManager.createPolicyFileName(policyName)
            val tmpTriggerFile = Paths.get(policyPath).parent.resolve("tmp").resolve(fileName).toFile()
            tmpTriggerFile.mkdirs()
            validatedMultipart.transferTo(tmpTriggerFile)

            try {
                validatePolicyFile(tmpTriggerFile)
            } catch (e: Throwable) {
                tmpTriggerFile.delete()
                if (createdPolicy)
                    myCustomTriggersManager.deleteCustomTriggerPolicy(policyDescriptor)
                throw e
            }

            Files.move(tmpTriggerFile.toPath(), Paths.get(policyPath), StandardCopyOption.REPLACE_EXISTING)

            myCustomTriggersManager.setTriggerPolicyUpdated(policyDescriptor, true)
            if (!createdPolicy)
                myCustomTriggersManager.setTriggerPolicyEnabled(policyDescriptor, true)

        } catch (e: Exception) {
            myLogger.warnAndDebugDetails("Failed to upload a trigger policy", e)
            model["error"] = e.message
            return modelAndView
        }

        return modelAndView
    }

    private fun SProject.hasLocalPolicy(policyName: String) =
        myCustomTriggersManager.localCustomTriggers(this).any { it.policyName == policyName }

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

    private fun validateUsages(
        project: SProject,
        policyName: String,
        updatedFileName: String,
        loadedFileName: String
    ) {
        val localPolicyExists = project.hasLocalPolicy(policyName)
        val descendant = project.projects.firstOrNull { it.hasLocalPolicy(policyName) }
        val ancestor = project.projectPath.dropLast(1)
            .firstOrNull { it.hasLocalPolicy(policyName) }

        val updateMode = updatedFileName.isNotBlank()

        if (updateMode && updatedFileName != loadedFileName)
            throw UploadException("Uploaded jar's filename doesn't match the updated file's name")

        if (!updateMode && localPolicyExists)
            throw UploadException("File is already loaded to this project")

        if (ancestor != null)
            throw UploadException("File is already loaded to this project's ancestor '${ancestor.fullName}'")

        if (descendant != null)
            throw UploadException("File is already loaded to this project's descendant '${descendant.fullName}'")
    }

    private fun validatePolicyFile(policyFile: File) {
        try {
            myPolicyFileManager.loadPolicyClass(policyFile.toPath(), false, {})
        } catch (e: ClassNotFoundException) {
            throw UploadException("Malformed policy archive: cannot find policy class")
        } catch (e: ClassCastException) {
            throw UploadException("Malformed policy archive: policy class is not an implementation of the ${CustomTriggerPolicy::class.qualifiedName} interface")
        }
    }

    private class UploadException(msg: String) : RuntimeException(msg)

    companion object {
        const val PATH = "/admin/uploadCustomTriggerPolicy.html"
    }
}