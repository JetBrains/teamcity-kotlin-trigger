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

package jetbrains.buildServer.buildTriggers.remote.ktor

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.request.ContentTransformationException
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.pipeline.*
import jetbrains.buildServer.buildTriggers.remote.TriggerPolicyManager
import jetbrains.buildServer.buildTriggers.remote.TriggerPolicyManagerImpl
import jetbrains.buildServer.buildTriggers.remote.jackson.TYPE_VALIDATOR
import jetbrains.buildServer.buildTriggers.remote.net.*
import java.nio.file.Paths
import java.util.logging.Level
import java.util.logging.Logger

internal typealias HandlingContext = PipelineContext<Unit, ApplicationCall>

internal class KtorServer(
    private val myHost: String,
    private val myPort: Int,
    myTriggerPolicyManager: TriggerPolicyManager
) {
    private val myLogger = Logger.getLogger(KtorServer::class.qualifiedName)
    private val myServer = createServer()
    private val myTriggerActions = TriggerActions(myTriggerPolicyManager, myLogger)

    init {
        myServer.start(wait = true)
    }

    private fun Routing.handleRoute(
        mapping: Mapping,
        block: suspend HandlingContext.(Unit) -> Unit
    ) = route(mapping.path, mapping.httpMethod) { handle(block) }

    private suspend fun <T : Any> HandlingContext.respondWithErrorsHandled(block: suspend () -> T): Unit =
        try {
            call.respond(block())
        } catch (wse: WrappingServerError) {
            wse.printStackTrace()
            call.respond(wse.asResponse())
        } catch (se: ServerError) {
            myLogger.severe(se.message)
            call.respond(se.asResponse())
        } catch (e: Throwable) {
            myLogger.log(Level.SEVERE, "Unknown internal server error", e)
            call.respond(internalServerError(e).asResponse())
        }

    private suspend inline fun <T : Any, reified R : RequestBody>
            HandlingContext.withTriggerNameAndRequestBody(block: (String, R) -> T): T {

        val triggerPolicyName = call.parameters["triggerPolicyName"] ?: throw noTriggerPolicyNameError()
        val requestBody = try {
            call.receive<R>()
        } catch (e: ContentTransformationException) {
            throw contentTypeMismatchError(e)
        }
        return block(triggerPolicyName, requestBody)
    }

    private fun createServer() = embeddedServer(Netty, host = myHost, port = myPort, configure = {
        callGroupSize = 1
        workerGroupSize = 1
        connectionGroupSize = 1
    }) {
        install(ContentNegotiation) {
            jackson {
                activateDefaultTyping(TYPE_VALIDATOR)
            }
        }
        routing {
            val triggerPolicyNameParam = "{triggerPolicyName}"

            handleRoute(RequestMapping.triggerBuild(triggerPolicyNameParam)) {
                respondWithErrorsHandled {
                    withTriggerNameAndRequestBody(myTriggerActions::triggerBuild)
                }
            }
            handleRoute(RequestMapping.uploadTriggerPolicy(triggerPolicyNameParam)) {
                respondWithErrorsHandled {
                    withTriggerNameAndRequestBody(myTriggerActions::saveTriggerPolicy)
                }
            }
        }
    }
}

fun main() {
    KtorServer("127.0.0.1", 8080, TriggerPolicyManagerImpl(Paths.get("triggerJars")))
}