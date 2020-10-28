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

package jetbrains.buildServer.buildTriggers.remote.annotation

@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class CustomTriggerProperties(vararg val properties: CustomTriggerProperty)

@Target(AnnotationTarget.CLASS)
@Repeatable
@MustBeDocumented
annotation class CustomTriggerProperty(
    val name: String,
    val type: PropertyType,
    val description: String,
    val required: Boolean = false
)

enum class PropertyType(val typeName: String) {
    TEXT("text"), BOOLEAN("boolean")
}
