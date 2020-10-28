/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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