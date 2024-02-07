

package jetbrains.buildServer.buildTriggers.remote.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.CustomTriggerPolicyDescriptor
import jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager
import jetbrains.buildServer.buildTriggers.remote.TriggerPolicyFileManager
import jetbrains.buildServer.buildTriggers.remote.annotation.CustomTriggerProperty
import jetbrains.buildServer.buildTriggers.remote.findProjectByRequest
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.controllers.BasePropertiesBean
import jetbrains.buildServer.parameters.ParametersUtil
import jetbrains.buildServer.serverSide.ControlDescription
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.parameters.WellknownParameterArguments
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.stereotype.Controller
import org.springframework.web.servlet.ModelAndView
import java.nio.file.Paths
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
internal class CustomTriggerPropertiesController(
    myWebControllerManager: WebControllerManager,
    private val myPluginDescriptor: PluginDescriptor,
    private val myProjectManager: ProjectManager,
    private val myCustomTriggersManager: CustomTriggersManager,
    private val myPolicyFileManager: TriggerPolicyFileManager<*>
) : BaseController() {

    private val myLogger = Logger.getInstance(CustomTriggerPropertiesController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(PATH, this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val project = myProjectManager.findProjectByRequest(request, myLogger)
            ?: throw RuntimeException("Cannot display custom trigger's properties: the request did not specify any project id")
        val policyName = request.getParameter("triggerPolicyName")
            ?: throw RuntimeException("Cannot display custom trigger's properties: the request did not specify any policy name")

        val policyDescriptor = CustomTriggerPolicyDescriptor(policyName, project)
        val policyPath = myCustomTriggersManager.getTriggerPolicyFilePath(policyDescriptor)
            ?: throw RuntimeException("Cannot display custom trigger's properties: policy '$policyName' does not exist")

        val properties = request.getParameter("properties")
            ?.let { deserializeMap(it) }
            ?: emptyMap()

        val parameters = mutableMapOf<String, ControlDescription>()
        val requiredMap = mutableMapOf<String, String>()
        myPolicyFileManager.loadPolicyClass(Paths.get(policyPath), false) { policyClass ->
            myPolicyFileManager
                .loadPolicyAnnotations(policyClass)
                .forEach {
                    it.addParameterTo(parameters, requiredMap)
                }
        }

        val mv = ModelAndView(myPluginDescriptor.getPluginResourcesPath("showCustomTriggerProperties.jsp"))
        mv.model["parameters"] = parameters
        mv.model["requiredMap"] = requiredMap
        mv.model["propertiesBean"] = BasePropertiesBean(properties)

        return mv
    }

    private fun CustomTriggerProperty.addParameterTo(
        parameters: MutableMap<String, ControlDescription>,
        requiredMap: MutableMap<String, String>
    ) {
        parameters[name] = ParametersUtil.createControlDescription(
            type.typeName,
            mapOf(
                WellknownParameterArguments.ARGUMENT_DESCRIPTION.name to description,
                WellknownParameterArguments.REQUIRED.name to required.toString()
            )
        )
        requiredMap[name] = required.toString()
    }

    companion object {
        const val PATH = "/admin/showCustomTriggerProperties.html"

        private val jackson = ObjectMapper()
        fun serializeMap(map: Map<String, String>): String = jackson.writeValueAsString(map)
        fun deserializeMap(source: String): Map<String, String> = jackson.readValue(source)
    }
}