package jetbrains.buildServer.buildTriggers.remote.jackson

import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import jetbrains.buildServer.buildTriggers.remote.Response
import jetbrains.buildServer.buildTriggers.remote.ServerError

object TypeValidator {
    val myTypeValidator: BasicPolymorphicTypeValidator =
        BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(Response::class.java)
            .allowIfSubType(ServerError::class.java)
            .allowIfSubType(Map::class.java)
            .build()
}