package jetbrains.buildServer.buildTriggers.remote

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

class NoTriggerPolicyNameError internal constructor(message: String) : ServerError(message)
class ContentTypeMismatchError internal constructor(message: String) : ServerError(message)
class TriggerPolicyDoesNotExistError internal constructor(message: String) : ServerError(message)
class TriggerPolicyLoadingError internal constructor(message: String) : ServerError(message)
class TriggerInvocationTimeoutError internal constructor(message: String) : ServerError(message)
class InternalTriggerPolicyError internal constructor(message: String) : ServerError(message)
class InternalServerError internal constructor(message: String) : ServerError(message)

/* These functions are needed to keep errors' constructors receive error message as the only parameter;
    otherwise, deserialization may break due to how Jackson decides what objects to pass to the constructor */
fun noTriggerPolicyNameError() = NoTriggerPolicyNameError("Trigger policy name not specified in request path")
fun contentTypeMismatchError(e: Throwable) = ContentTypeMismatchError("Request body expected to be of type: $e")
fun triggerPolicyDoesNotExistError(triggerName: String) =
    TriggerPolicyDoesNotExistError("Trigger policy '$triggerName' does not exist")

fun triggerPolicyLoadingError(e: Throwable) = TriggerPolicyLoadingError("Exception while loading a trigger policy: $e")
fun triggerInvocationTimeoutError(triggerName: String) =
    TriggerInvocationTimeoutError("Time limit exceeded on trigger $triggerName invocation")

fun internalTriggerPolicyError(e: Throwable) = InternalTriggerPolicyError("Trigger invocation caused an exception: $e")
fun internalServerError(e: Throwable) = InternalServerError("Internal server error: $e")
