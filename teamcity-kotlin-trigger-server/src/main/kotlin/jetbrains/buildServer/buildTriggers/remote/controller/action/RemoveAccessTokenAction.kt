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

package jetbrains.buildServer.buildTriggers.remote.controller.action

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.CustomTriggerPolicyDescriptor
import jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager
import jetbrains.buildServer.buildTriggers.remote.controller.PolicyAction
import jetbrains.buildServer.buildTriggers.remote.findProjectByRequest
import jetbrains.buildServer.serverSide.ProjectManager
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class RemoveAccessTokenAction(
    private val myProjectManager: ProjectManager,
    private val myCustomTriggersManager: CustomTriggersManager
) : PolicyAction(
    ACTION,
    ERROR_KEY
) {
    private val myLogger = Logger.getInstance(RemoveAccessTokenAction::class.qualifiedName)

    companion object {
        const val ACTION = "removeAccessToken"
        const val ERROR_KEY = "removeAccessTokenError"
    }

    override fun canProcess(request: HttpServletRequest) = super.canProcess(request) &&
            request.getParameter("policyName") != null &&
            myProjectManager.findProjectByRequest(request, myLogger) != null

    override fun processPost(request: HttpServletRequest, response: HttpServletResponse) {
        val project = myProjectManager.findProjectByRequest(request, myLogger) ?: return
        val policyName = request.getParameter("policyName") ?: return
        val policyDescriptor = CustomTriggerPolicyDescriptor(policyName, project)

        myCustomTriggersManager.deleteTriggerPolicyAuthToken(policyDescriptor)
    }
}
