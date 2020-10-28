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

package jetbrains.buildServer.buildTriggers.remote.net

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(NoTriggerPolicyNameError::class),
    JsonSubTypes.Type(ContentTypeMismatchError::class),
    JsonSubTypes.Type(TriggerPolicyDoesNotExistError::class),
    JsonSubTypes.Type(TriggerPolicyLoadingError::class),
    JsonSubTypes.Type(InternalTriggerPolicyError::class),
    JsonSubTypes.Type(InternalServerError::class)
)
sealed class ServerError(override val message: String) : RuntimeException(message) {
    fun asResponse() = ErroneousResponse(this)
    override fun toString() = message
}

sealed class WrappingServerError(message: String, override val cause: Throwable): ServerError("$message: $cause")

class NoTriggerPolicyNameError internal constructor(message: String) : ServerError(message)
class TriggerPolicyDoesNotExistError internal constructor(message: String) : ServerError(message)
class TriggerInvocationTimeoutError internal constructor(message: String) : ServerError(message)

class ContentTypeMismatchError internal constructor(message: String, cause: Throwable) : WrappingServerError(message, cause)
class TriggerPolicyLoadingError internal constructor(message: String, cause: Throwable) : WrappingServerError(message, cause)
class InternalTriggerPolicyError internal constructor(message: String, cause: Throwable) : WrappingServerError(message, cause)
class InternalServerError internal constructor(message: String, cause: Throwable) : WrappingServerError(message, cause)

/* These functions are needed to keep errors' constructors receive error message as the only String parameter;
    otherwise, deserialization may break due to how Jackson decides what objects to pass to the constructor */
fun noTriggerPolicyNameError() = NoTriggerPolicyNameError("Trigger policy name not specified in request path")
fun triggerPolicyDoesNotExistError(triggerName: String) =
    TriggerPolicyDoesNotExistError("Trigger policy '$triggerName' does not exist")
fun triggerInvocationTimeoutError(triggerName: String) =
    TriggerInvocationTimeoutError("Time limit exceeded on trigger $triggerName invocation")

fun contentTypeMismatchError(e: Throwable) = ContentTypeMismatchError("Request body expected to be of another type", e)
fun triggerPolicyLoadingError(e: Throwable) = TriggerPolicyLoadingError("Exception while loading a trigger policy", e)
fun internalTriggerPolicyError(e: Throwable) = InternalTriggerPolicyError("Trigger invocation caused an exception", e)
fun internalServerError(e: Throwable) = InternalServerError("Internal server error", e)
