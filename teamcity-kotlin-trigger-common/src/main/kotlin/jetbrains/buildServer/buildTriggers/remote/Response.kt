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

class ErroneousResponse(val error: ServerError) : Response()
class TriggerBuildResponse(val answer: Boolean) : Response()
object UploadTriggerResponse : Response()
