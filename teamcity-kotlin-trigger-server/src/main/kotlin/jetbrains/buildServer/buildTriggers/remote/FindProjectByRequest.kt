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
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SProject
import javax.servlet.http.HttpServletRequest

private const val BT_PREFIX = "buildType:"
private const val TEMPLATE_PREFIX = "template:"

fun ProjectManager.findProjectByRequest(request: HttpServletRequest, logger: Logger): SProject? {
    val projectId = request.getParameter("projectId")

    if (projectId != null) {
        val project = findProjectByExternalId(projectId)

        if (project != null) return project
        else logger.warn("No project found by project id '$projectId'")
    }

    val id = request.getParameter("id") ?: return null

    return when {
        id.startsWith(BT_PREFIX) -> {
            val buildTypeId = id.substring(BT_PREFIX.length)
            val buildType = findBuildTypeByExternalId(buildTypeId)

            buildType?.project
                ?: run {
                    logger.warn("No build type found by id '$buildTypeId'")
                    null
                }
        }
        id.startsWith(TEMPLATE_PREFIX) -> {
            val templateId = id.substring(TEMPLATE_PREFIX.length)
            val template = findBuildTypeTemplateByExternalId(templateId)

            template?.project
                ?: run {
                    logger.warn("No template found by id '$templateId'")
                    null
                }
        }
        else -> {
            val buildType = findBuildTypeByExternalId(id)
            val template = findBuildTypeTemplateByExternalId(id)

            buildType?.project
                ?: template?.project
                ?: run {
                    logger.warn("Cannot obtain current project: id '$id' does not belong to neither project, build type, nor template")
                    null
                }
        }
    }
}