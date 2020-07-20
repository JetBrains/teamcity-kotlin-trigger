package jetbrains.buildServer.buildTriggers.remote

import java.io.File
import java.net.URLClassLoader

object TriggerManager {
    internal fun loadTrigger(triggerName: String): Trigger {
        if (!triggerExists(triggerName))
            throw TriggerDoesNotExistException()

        val urlClassLoader = URLClassLoader(arrayOf(triggerPath(triggerName)))
        val triggerClass = Class.forName(triggerClass(triggerName), true, urlClassLoader)

        if (!Trigger::class.java.isAssignableFrom(triggerClass))
            throw ClassCastException("$triggerClass cannot be cast to ${Trigger::class.qualifiedName}")

        val instance = triggerClass.getDeclaredConstructor().newInstance()
        return instance as Trigger
    }

    internal fun saveTrigger(triggerName: String, bytes: ByteArray) {
        File(triggerPath(triggerName).toURI())
            .writeBytes(bytes)
    }

    private fun triggerExists(triggerName: String) = File("triggers/$triggerName.jar").exists()
    private fun triggerPath(triggerName: String) = File("triggers/$triggerName.jar").toURI().toURL()
    private fun triggerClass(triggerName: String) = "jetbrains.buildServer.buildTriggers.remote.compiled.$triggerName"

    class TriggerDoesNotExistException: RuntimeException()
}