package jetbrains.buildServer.buildTriggers.remote

import java.net.URLClassLoader
import java.nio.file.Path

internal class TriggerPolicyManagerImpl(private val myTriggerPolicyDirPath: Path) : TriggerPolicyManager {
    private val myTriggerPolicyMap = mutableMapOf<String, CustomTriggerPolicy>()

    override fun loadTriggerPolicy(triggerPolicyName: String): CustomTriggerPolicy = myTriggerPolicyMap
        .computeIfAbsent(triggerPolicyName) {
            if (!triggerPolicyExists(triggerPolicyName))
                throw TriggerPolicyDoesNotExistException()

            val urlClassLoader = URLClassLoader(arrayOf(triggerPolicyUrl(triggerPolicyName)))
            val triggerPolicyClass = Class.forName(triggerPolicyClass(triggerPolicyName), true, urlClassLoader)

            triggerPolicyClass.getDeclaredConstructor().newInstance() as CustomTriggerPolicy
        }

    override fun saveTriggerPolicy(triggerPolicyName: String, bytes: ByteArray) {
        myTriggerPolicyDirPath.toFile().mkdir()
        triggerPolicyPath(triggerPolicyName)
            .toFile()
            .writeBytes(bytes)
    }

    private fun triggerPolicyPath(triggerPolicyName: String) = myTriggerPolicyDirPath.resolve("$triggerPolicyName.jar")
    private fun triggerPolicyExists(triggerPolicyName: String) = triggerPolicyPath(triggerPolicyName).toFile().exists()
    private fun triggerPolicyUrl(triggerPolicyName: String) = triggerPolicyPath(triggerPolicyName).toUri().toURL()
    private fun triggerPolicyClass(triggerPolicyName: String) =
        "jetbrains.buildServer.buildTriggers.remote.compiled.$triggerPolicyName"
}