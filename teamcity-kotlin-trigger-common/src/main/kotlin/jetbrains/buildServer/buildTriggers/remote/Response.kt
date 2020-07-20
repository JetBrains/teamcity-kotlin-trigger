package jetbrains.buildServer.buildTriggers.remote

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(ErroneousResponse::class),
    JsonSubTypes.Type(TriggerBuildResponse::class),
    JsonSubTypes.Type(UploadTriggerResponse::class)
)
sealed class Response

data class ErroneousResponse(val error: ServerError) : Response()
class TriggerBuildResponse(val answer: Boolean) : Response()
object UploadTriggerResponse : Response()

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(NoTriggerNameError::class),
    JsonSubTypes.Type(ContentTypeMismatch::class),
    JsonSubTypes.Type(TriggerDoesNotExistError::class),
    JsonSubTypes.Type(TriggerLoadingError::class),
    JsonSubTypes.Type(InternalTriggerError::class),
    JsonSubTypes.Type(InternalServerError::class)
)
sealed class ServerError(msg: String) : RuntimeException(msg) {
    fun asResponse() = ErroneousResponse(this)
    override fun toString() = message!!
}

class NoTriggerNameError : ServerError("Trigger name not specified in request path")
class ContentTypeMismatch(e: Throwable) : ServerError("Request body cannot be transformed to the expected type: $e")

class TriggerDoesNotExistError(triggerName: String) : ServerError("Trigger '$triggerName' does not exist")
class TriggerLoadingError(e: Throwable) : ServerError("Exception while loading trigger: $e")
class InternalTriggerError(e: Throwable) : ServerError("Trigger invocation caused an exception: $e")
class InternalServerError(e: Throwable) : ServerError("Internal server error: $e")
