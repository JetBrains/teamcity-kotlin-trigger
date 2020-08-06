package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
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
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

@Controller
internal class CustomTriggerPropertiesController(
    myWebControllerManager: WebControllerManager,
    private val myPluginDescriptor: PluginDescriptor
) : BaseController() {

    private val myLogger = Logger.getInstance(CustomTriggerPropertiesController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(PATH, this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        println("HANDLE")
        val triggerPolicyPath = request.getParameter("triggerPolicyPath")
            ?: run {
                myLogger.error("Cannot display custom trigger's properties: the request did not specify any filepath")
                return null
            }
        println("HANDLE PATH $triggerPolicyPath")

//        val policyFile = File(triggerPolicyPath)
        val file = File("D:\\Docs\\Programmes\\workspace_IDEA\\JetBrains\\TeamCity_Data\\config\\projects\\Spellchecker\\pluginData\\teamcityKotlinTrigger\\ScheduleTriggerPolicy.jar")
        val classLoader = URLClassLoader(arrayOf(file.toURI().toURL()))

        val policyClassName = "jetbrains.buildServer.buildTriggers.remote.compiled.${file.nameWithoutExtension}"

        val triggerPolicyClass = Class.forName(policyClassName, false, classLoader)
//        println("LIKE IN CODERUNNER: " + triggerPolicyClass.kotlin.findAnnotation<CustomTriggerProperty>()?.description)

        val parameters = mutableMapOf<String, ControlDescription>()

        println("COMPARISON BEGIN")
        println(System.getProperty("java.version"))


        val ann = triggerPolicyClass.kotlin.annotations.first()
        println("is WORKS: " + (ann is CustomTriggerProperty).toString())

        val ann1 = ann::class
        val ann2 = CustomTriggerProperty::class

        println(ann1.supertypes.joinToString { it.toString() } + " ----- " + ann2.supertypes.joinToString { it.toString() })
        println(ann1.qualifiedName + " ----- " + ann2.qualifiedName)
        println(ann1.isInstance(ann).toString() + " ----- " + ann2.isInstance(ann).toString())
        println(ann1.visibility.toString() + " ----- " + ann2.visibility.toString())
        println(ann1.javaObjectType.name + " ----- " + ann2.javaObjectType.name)
        println(ann1.java.name + " ----- " + ann2.java.name)
        println(ann1::class.qualifiedName + " ----- " + ann2::class.qualifiedName)

        println("COMPARISON END")
        println("HANDLE START PARAMETERS")

//        triggerPolicyClass.annotations
//            .filter { // maybe due to 'false' in Class#forName invocation, filterIsInstance does not work
//                it.annotationClass.qualifiedName == CustomTriggerProperties::class.qualifiedName
//            }
//            .map(this::createCustomTriggerPropertyFrom)
//            .flatMap { it.properties.asIterable() }
//            .forEach { property ->
//                println("PROPERTIES: ${property.name}")
//                property.addToParameterMap(parameters)
//            }

        println("FIND ANN BY TYPE: " + triggerPolicyClass.kotlin.findAnnotation<CustomTriggerProperty>()?.description)

        triggerPolicyClass.kotlin.annotations
            .filter {
                println("JAVA CLASS" + it.javaClass.name)
                println("ANNOTATION CLASS" + it.annotationClass.qualifiedName)
                println(it is CustomTriggerProperty)
                println(it.annotationClass.memberProperties.joinToString { it.name })
                println(it.annotationClass.memberFunctions.joinToString { it.name })
                it.annotationClass.qualifiedName == CustomTriggerProperty::class.qualifiedName
            }
        println("-------------")
//        triggerPolicyClass.kotlin.annotations
        triggerPolicyClass.annotations
            .filter {
                println("JAVA CLASS" + it.javaClass.name)
                println("ANNOTATION CLASS" + it.annotationClass.qualifiedName)
                it.annotationClass.qualifiedName == CustomTriggerProperty::class.qualifiedName
            }
            .map(this::createCustomTriggerPropertyFrom)
            .forEach { property ->
                println("PROPERTY: ${property.name}")
                property.addToParameterMap(parameters)
            }
        println("HANDLE END PARAMETERS")
        println()

        val mv = ModelAndView(myPluginDescriptor.getPluginResourcesPath("showCustomTriggerProperties.jsp"))
//        val propertiesBean = EditBuildTypeFormFactory
//BuildTriggerDescriptorBean
        mv.model["parameters"] = parameters
        mv.model["propertiesBean"] = BasePropertiesBean(mutableMapOf())
        return mv
    }

    private fun createCustomTriggerPropertyFrom(o: Annotation): CustomTriggerPropertyBean {
        val jklass = o.javaClass

        println("FIELDS JAVA: " + jklass.declaredFields.joinToString { it.name })
        println("METHODS JAVA: " + jklass.declaredMethods.joinToString { it.name })


        val klass = o.annotationClass.java

        println("FIELDS: " + klass.declaredFields.joinToString { it.name })
        println("METHODS: " + klass.declaredMethods.joinToString { it.name })

        val name = klass.getMethod("name").invoke(o) as String

        val type = klass.getMethod("type").invoke(o)

        println("FIELDS: " + type.javaClass.fields.joinToString { it.name })
        println("METHODS: " + type.javaClass.methods.joinToString { it.name })

        val typeName = type.javaClass.getMethod("getTypeName").invoke(type) as String

        val description = klass.getMethod("description").invoke(o) as String
        val required = klass.getMethod("required").invoke(o) as Boolean

        println("WOOOORKS: $name $typeName $description $required")

        return CustomTriggerPropertyBean(name, typeName, description, required)
    }


    private fun CustomTriggerPropertyBean.addToParameterMap(parameters: MutableMap<String, ControlDescription>) {
        parameters[name] = ParametersUtil.createControlDescription(
            typeName,
            mapOf(
                WellknownParameterArguments.ARGUMENT_DESCRIPTION.name to description,
                WellknownParameterArguments.REQUIRED.name to required.toString()
            )
        )
    }

    companion object {
        const val PATH = "/admin/showCustomTriggerProperties.html"
    }
}

private data class CustomTriggerPropertyBean(
    val name: String,
    val typeName: String,
    val description: String,
    val required: Boolean
)
