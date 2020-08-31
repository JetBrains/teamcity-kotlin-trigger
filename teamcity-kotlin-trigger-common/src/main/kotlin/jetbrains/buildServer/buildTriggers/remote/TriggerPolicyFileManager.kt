package jetbrains.buildServer.buildTriggers.remote

import jetbrains.buildServer.buildTriggers.remote.annotation.CustomTriggerProperties
import jetbrains.buildServer.buildTriggers.remote.annotation.CustomTriggerProperty
import java.io.File
import java.io.FileNotFoundException
import java.net.URLClassLoader
import java.nio.file.Path

interface TriggerPolicyFileManager<T> {
    @Throws(FileNotFoundException::class, WrongFileFormatException::class)
    fun getPolicyName(filePath: Path): String

    /** Loads the class, initializes it if necessary, and invokes a callback on it.
     * A callback is needed, because the file is loaded from a file and thus should be closed in the end. */
    @Throws(
        FileNotFoundException::class,
        WrongFileFormatException::class,
        ClassNotFoundException::class,
        ClassCastException::class
    )
    fun <T> loadPolicyClass(filePath: Path, initialize: Boolean, onLoad: (Class<out CustomTriggerPolicy>) -> T): T

    fun loadPolicyAnnotations(policyClass: Class<out CustomTriggerPolicy>): Iterable<CustomTriggerProperty>

    /** This method is for deciding what should be the name of the file, containing a policy.
     * [context] is anything that might help with deciding, and implementation-dependant thing. */
    fun createPolicyFileName(context: T): String

    class WrongFileFormatException(message: String): RuntimeException("Wrong policy file format: $message")
}

/** A simple implementation of a policy file manager, that does not need any configuration files.
 * This implementation expects the policy name (as well as the class name) be equal to the filename (without .jar extension).
 * Also, the policy class's package is expected to be jetbrains.buildServer.buildTriggers.remote.compiled */
class SimpleTriggerPolicyJarManager : TriggerPolicyFileManager<String> {
    private val jarExtension = ".jar"

    @Throws(TriggerPolicyFileManager.WrongFileFormatException::class)
    override fun getPolicyName(filePath: Path): String {
        val policyFile = filePath.toFile()
        if (!policyFile.name.endsWith(jarExtension))
            throw TriggerPolicyFileManager.WrongFileFormatException("Path '$filePath' does not belong to a jar file")

        return policyFile.nameWithoutExtension
    }

    @Throws(
        FileNotFoundException::class,
        TriggerPolicyFileManager.WrongFileFormatException::class,
        ClassNotFoundException::class,
        ClassCastException::class
    )
    override fun <T> loadPolicyClass(filePath: Path, initialize: Boolean, onLoad: (Class<out CustomTriggerPolicy>) -> T): T {
        val policyClassName = getPolicyClassName(getPolicyName(filePath))
        val policyUrl = findFile(filePath).toURI().toURL()

        // this is to be sure that the annotations will be visible
        val parentClassLoader = CustomTriggerProperties::class.java.classLoader
        val urlClassLoader = URLClassLoader(arrayOf(policyUrl), parentClassLoader)

        return urlClassLoader.use { classLoader ->
            val policyClass = Class.forName(policyClassName, initialize, classLoader)

            if (!CustomTriggerPolicy::class.java.isAssignableFrom(policyClass))
                throw RuntimeException("Cannot display custom trigger's properties: the class of the policy is not an implementation of the ${CustomTriggerPolicy::class.qualifiedName} interface")

            val castedPolicyClass = policyClass.asSubclass(CustomTriggerPolicy::class.java)
            onLoad(castedPolicyClass)
        }
    }

    override fun loadPolicyAnnotations(policyClass: Class<out CustomTriggerPolicy>): Iterable<CustomTriggerProperty> =
        sequence<CustomTriggerProperty> {
            policyClass.annotations.forEach { annotation ->
                when (annotation) {
                    is CustomTriggerProperty -> yield(annotation)
                    is CustomTriggerProperties -> yieldAll(annotation.properties.iterator())
                }
            }
        }.asIterable()

    override fun createPolicyFileName(policyName: String) =
        "$policyName$jarExtension"

    private fun findFile(filePath: Path): File {
        val policyFile = filePath.toFile()
        if (!policyFile.exists())
            throw FileNotFoundException("File $filePath does not exist")

        return policyFile
    }

    private fun getPolicyClassName(policyName: String) =
        "jetbrains.buildServer.buildTriggers.remote.compiled.$policyName"
}
