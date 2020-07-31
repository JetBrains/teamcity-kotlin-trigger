package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.controllers.MultipartFormController
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.PluginException
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.ModelAndView
import java.io.File
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class UploadTriggerController(
    private val myProjectManager: ProjectManager,
    private val myPluginDescriptor: PluginDescriptor,
    myWebControllerManager: WebControllerManager
) : MultipartFormController() {
    private val myLogger = Logger.getInstance(UploadTriggerController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(
            "/admin/uploadCustomTrigger.html",
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

        val bean = CustomTriggersBean(myPluginDescriptor, project)
        val triggerJarName = request.getParameter("fileName")

        try {
            if (bean.getFiles().any { it.name == triggerJarName })
                throw PluginException("File already loaded")

            validateMultipart(request, triggerJarName)
                .transferTo(File(bean.pluginDataDirectory, triggerJarName))
        } catch (e: Exception) {
            myLogger.warnAndDebugDetails("Error while uploading plugin", e)
            model["error"] = e.message
            return modelAndView
        }

        return modelAndView
    }

    private fun validateMultipart(
        request: HttpServletRequest,
        triggerJarName: String
    ): MultipartFile {
        val triggerJar = getMultipartFileOrFail(request, "file:fileToUpload")

        if (triggerJar == null || triggerJar.isEmpty)
            throw PluginException("The file is not selected")

        if (StringUtil.isEmpty(triggerJarName))
            throw PluginException("The file name is not specified")

        if (!triggerJarName.endsWith(".jar"))
            throw PluginException("Selected file must be a JAR archive")

        return triggerJar
    }
}
