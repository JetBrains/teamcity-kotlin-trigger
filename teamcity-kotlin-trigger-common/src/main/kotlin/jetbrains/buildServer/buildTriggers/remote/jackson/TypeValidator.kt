

package jetbrains.buildServer.buildTriggers.remote.jackson

import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import jetbrains.buildServer.buildTriggers.remote.net.Response
import jetbrains.buildServer.buildTriggers.remote.net.ServerError

val TYPE_VALIDATOR: BasicPolymorphicTypeValidator = BasicPolymorphicTypeValidator.builder()
    .allowIfSubType(Response::class.java)
    .allowIfSubType(ServerError::class.java)
    .allowIfSubType(Map::class.java)
    .allowIfSubType(List::class.java)
    .build()