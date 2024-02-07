

package jetbrains.buildServer.buildTriggers.remote

import java.io.FileNotFoundException
import java.nio.file.Path

internal class TriggerPolicyManagerImpl(private val myTriggerPolicyDirPath: Path) : TriggerPolicyManager {
    private val myPolicyFileManager = SimpleTriggerPolicyJarManager()

    override fun <T> loadTriggerPolicy(policyName: String, onLoad: (CustomTriggerPolicy) -> T): T {
        val policyFilePath = triggerPolicyPath(policyName)

        return try {
            myPolicyFileManager.loadPolicyClass(policyFilePath, true) { policyClass ->
                val policy = policyClass.getDeclaredConstructor().newInstance()
                onLoad(policy)
            }
        } catch (e: FileNotFoundException) {
            throw TriggerPolicyDoesNotExistException()
        }
    }

    override fun saveTriggerPolicy(policyName: String, bytes: ByteArray) {
        myTriggerPolicyDirPath.toFile().mkdir()
        triggerPolicyPath(policyName)
            .toFile()
            .writeBytes(bytes)
    }

    private fun triggerPolicyPath(policyName: String) =
        myTriggerPolicyDirPath.resolve(myPolicyFileManager.createPolicyFileName(policyName))
}