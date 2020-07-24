package jetbrains.buildServer.buildTriggers.remote

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(NoTriggerNameError::class),
    JsonSubTypes.Type(ContentTypeMismatchError::class),
    JsonSubTypes.Type(TriggerDoesNotExistError::class),
    JsonSubTypes.Type(TriggerLoadingError::class),
    JsonSubTypes.Type(InternalTriggerError::class),
    JsonSubTypes.Type(InternalServerError::class)
)
sealed class ServerError(msg: String) : RuntimeException(msg) {
    fun asResponse() = ErroneousResponse(this)
    override fun toString() = message!!
}
// TODO: think about property errors
class NoTriggerNameError internal constructor(msg: String) : ServerError(msg)
class ContentTypeMismatchError internal constructor(msg: String) : ServerError(msg)
class TriggerDoesNotExistError internal constructor(msg: String) : ServerError(msg)
class TriggerLoadingError internal constructor(msg: String) : ServerError(msg)
class InternalTriggerError internal constructor(msg: String) : ServerError(msg)
class InternalServerError internal constructor(msg: String) : ServerError(msg)

/* These functions are needed to keep errors' constructors receive error message as the only parameter;
    otherwise, deserialization may break due to how Jackson decides what objects to pass to the constructor */
fun noTriggerNameError() = NoTriggerNameError("Trigger name not specified in request path")
fun contentTypeMismatchError(e: Throwable) = ContentTypeMismatchError("Request body expected to be of type: $e")
fun triggerDoesNotExistError(triggerName: String) = TriggerDoesNotExistError("Trigger '$triggerName' does not exist")
fun triggerLoadingError(e: Throwable) = TriggerLoadingError("Exception while loading trigger: $e")
fun internalTriggerError(e: Throwable) = InternalTriggerError("Trigger invocation caused an exception: $e")
fun internalServerError(e: Throwable) = InternalServerError("Internal server error: $e")
