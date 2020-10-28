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

import jetbrains.buildServer.serverSide.SProject

/** Represents a trigger policy descriptor in a scope of some project */
data class CustomTriggerPolicyDescriptor(
    val policyName: String, // unique in current project's scope (i.e. the project itself and all its descendants)
    val project: SProject // some project where the policy is visible
)
