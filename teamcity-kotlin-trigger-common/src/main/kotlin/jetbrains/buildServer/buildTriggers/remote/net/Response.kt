

package jetbrains.buildServer.buildTriggers.remote.net

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
    JsonSubTypes.Type(OkayResponse::class)
)
sealed class Response

class ErroneousResponse(val error: ServerError) : Response()
class TriggerBuildResponse(val answer: Boolean, val customData: Map<String, String>) : Response()
object OkayResponse : Response()