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

package jetbrains.buildServer.buildTriggers.remote.controller

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager
import jetbrains.buildServer.buildTriggers.remote.findProjectByRequest
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.stereotype.Controller
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal const val CUSTOM_TRIGGERS_LIST_CONTROLLER = "customTriggersListController.html"

/** Provides the trigger policy selection screen with data needed to obtain all currently visible policies */
@Controller
class CustomTriggersListController(
    private val myProjectManager: ProjectManager,
    private val myPluginDescriptor: PluginDescriptor,
    private val myCustomTriggersManager: CustomTriggersManager,
    myWebControllerManager: WebControllerManager
) : BaseController() {

    private val myLogger = Logger.getInstance(CustomTriggersListController::class.qualifiedName)

    init {
        myWebControllerManager.registerController(
            myPluginDescriptor.getPluginResourcesPath(CUSTOM_TRIGGERS_LIST_CONTROLLER),
            this
        )
    }

    override fun doHandle(req: HttpServletRequest, res: HttpServletResponse): ModelAndView? {
        val mv = ModelAndView(myPluginDescriptor.getPluginResourcesPath("customTriggerPolicyProperties.jsp"))
        val project = myProjectManager.findProjectByRequest(req, myLogger)
            ?: throw RuntimeException("The request did not specify any project id")

        mv.model["customTriggersManager"] = myCustomTriggersManager
        mv.model["project"] = project

        return mv
    }
}
