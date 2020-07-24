package jetbrains.buildServer.buildTriggers.remote

import java.net.URLClassLoader
import java.nio.file.Path

internal class TriggerManagerImpl(private val myTriggerDirPath: Path) : TriggerManager {
    // TODO: store in a map to not instantiate a new trigger each time
    override fun loadTrigger(triggerName: String): TriggerService {
        if (!triggerExists(triggerName))
            throw TriggerDoesNotExistException()

        val urlClassLoader = URLClassLoader(arrayOf(triggerURL(triggerName)))
        val triggerClass = Class.forName(triggerClass(triggerName), true, urlClassLoader)

        if (!TriggerService::class.java.isAssignableFrom(triggerClass))
            throw ClassCastException("$triggerClass cannot be cast to ${TriggerService::class.qualifiedName}")

        val instance = triggerClass.getDeclaredConstructor().newInstance()
        return instance as TriggerService
    }

    override fun saveTrigger(triggerName: String, bytes: ByteArray) {
        myTriggerDirPath.toFile().mkdir()
        triggerPath(triggerName)
            .toFile()
            .writeBytes(bytes)
    }

    private fun triggerPath(triggerName: String) = myTriggerDirPath.resolve("$triggerName.jar")
    private fun triggerExists(triggerName: String) = triggerPath(triggerName).toFile().exists()
    private fun triggerURL(triggerName: String) = triggerPath(triggerName).toUri().toURL()
    private fun triggerClass(triggerName: String) = "jetbrains.buildServer.buildTriggers.remote.compiled.$triggerName"
}