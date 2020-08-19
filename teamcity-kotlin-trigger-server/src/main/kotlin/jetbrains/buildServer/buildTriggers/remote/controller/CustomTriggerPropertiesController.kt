package jetbrains.buildServer.buildTriggers.remote.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jetbrains.buildServer.buildTriggers.remote.CustomTriggerPolicy
import jetbrains.buildServer.buildTriggers.remote.annotation.CustomTriggerProperties
import jetbrains.buildServer.buildTriggers.remote.annotation.CustomTriggerProperty
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.controllers.BasePropertiesBean
import jetbrains.buildServer.parameters.ParametersUtil
import jetbrains.buildServer.serverSide.ControlDescription
import jetbrains.buildServer.serverSide.parameters.WellknownParameterArguments
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.stereotype.Controller
import org.springframework.web.servlet.ModelAndView
import java.io.File
import java.net.URLClassLoader
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
internal class CustomTriggerPropertiesController(
    myWebControllerManager: WebControllerManager,
    private val myPluginDescriptor: PluginDescriptor
) : BaseController() {

    init {
        myWebControllerManager.registerController(PATH, this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val triggerPolicyPath = request.getParameter("triggerPolicyPath")
            ?: throw RuntimeException("Cannot display custom trigger's properties: the request did not specify any filepath")

        val properties = request.getParameter("properties")
            ?.let { deserializeMap(it) }
            ?: emptyMap()

        val triggerPolicyClass = loadPolicyClass(triggerPolicyPath)

        val parameters = mutableMapOf<String, ControlDescription>()
        val requiredMap = mutableMapOf<String, String>()

        triggerPolicyClass.annotations.forEach { annotation ->
            when (annotation) {
                is CustomTriggerProperty -> annotation.addParameterTo(parameters, requiredMap)
                is CustomTriggerProperties -> annotation.properties.forEach { it.addParameterTo(parameters, requiredMap) }
            }
        }

        val mv = ModelAndView(myPluginDescriptor.getPluginResourcesPath("showCustomTriggerProperties.jsp"))
        mv.model["parameters"] = parameters
        mv.model["requiredMap"] = requiredMap
        mv.model["propertiesBean"] = BasePropertiesBean(properties)

        return mv
    }

    private fun loadPolicyClass(policyPath: String): Class<*> {
        val file = File(policyPath)
        val classLoader = URLClassLoader(arrayOf(file.toURI().toURL()), CustomTriggerProperty::class.java.classLoader)

        val policyClassName = "jetbrains.buildServer.buildTriggers.remote.compiled.${file.nameWithoutExtension}"

        return try {
            val klass = Class.forName(policyClassName, false, classLoader)
            if (CustomTriggerPolicy::class.java.isAssignableFrom(klass))
                throw RuntimeException("Cannot display custom trigger's properties: the class of the policy is not an implementation of the ${CustomTriggerPolicy::class.qualifiedName} interface")
            klass
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("Cannot display custom trigger's properties: cannot find the class of the policy")
        }
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
