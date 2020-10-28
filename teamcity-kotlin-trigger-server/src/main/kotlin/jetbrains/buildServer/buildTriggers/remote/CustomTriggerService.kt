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

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor
import jetbrains.buildServer.buildTriggers.BuildTriggerService
import jetbrains.buildServer.buildTriggers.async.AsyncPolledBuildTriggerFactory
import jetbrains.buildServer.buildTriggers.remote.controller.CUSTOM_TRIGGERS_LIST_CONTROLLER
import jetbrains.buildServer.buildTriggers.remote.controller.CustomTriggerPropertiesController
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.util.TimeService
import jetbrains.buildServer.web.openapi.PluginDescriptor
import org.springframework.stereotype.Service

@Service
class CustomTriggerService(
    factory: AsyncPolledBuildTriggerFactory,
    timeService: TimeService,
    private val myPluginDescriptor: PluginDescriptor,
    private val myProjectManager: ProjectManager,
    private val myCustomTriggersManager: CustomTriggersManager
) : BuildTriggerService() {

    private val myPolicy = factory.createBuildTrigger(
        RemoteTriggerPolicy(timeService, myCustomTriggersManager),
        Logger.getInstance(CustomTriggerService::class.qualifiedName)
    )

    override fun getName() = "teamcityKotlinTrigger"
    override fun getDisplayName() = "Custom Trigger..."

    override fun describeTrigger(buildTriggerDescriptor: BuildTriggerDescriptor): String {
        val properties = buildTriggerDescriptor.properties
        val policyName = TriggerUtil.getTargetTriggerPolicyName(properties)
            ?: return "Trigger policy is not selected"

        val projectId = buildTriggerDescriptor.properties["projectId"] ?: return "Project id cannot be determined"
        val project = myProjectManager.findProjectByExternalId(projectId) ?: return "Project cannot be determined"

        val policyDescriptor = CustomTriggerPolicyDescriptor(policyName, project)

        val disabledStatus =
            if (myCustomTriggersManager.isTriggerPolicyEnabled(policyDescriptor)) ""
            else "(disabled)"

        return "Uses $policyName $disabledStatus"
    }

    override fun getTriggerPropertiesProcessor() = PropertiesProcessor { properties: Map<String, String> ->
        val policyName = TriggerUtil.getTargetTriggerPolicyName(properties)

        val requiredMap = properties["requiredMap"]
            ?.let { CustomTriggerPropertiesController.deserializeMap(it) }
            ?: emptyMap()

        val errors = mutableListOf<InvalidProperty>()

        fun addInvalidProperty(s1: String, s2: String) = errors.add(InvalidProperty(s1, s2))

        if (policyName.isNullOrBlank())
            addInvalidProperty(TriggerUtil.TRIGGER_POLICY_NAME, "A trigger policy should be specified")

        for ((propertyName, requiredStr) in requiredMap) {
            if (StringUtil.isTrue(requiredStr) && properties[propertyName].isNullOrEmpty()) {
                addInvalidProperty(propertyName, "This property is required")
            }
        }

        errors
    }

    override fun getEditParametersUrl() =
        myPluginDescriptor.getPluginResourcesPath(CUSTOM_TRIGGERS_LIST_CONTROLLER)

    override fun getBuildTriggeringPolicy() = myPolicy
    override fun isMultipleTriggersPerBuildTypeAllowed() = true
}
