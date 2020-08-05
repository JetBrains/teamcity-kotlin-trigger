package jetbrains.buildServer.buildTriggers.remote.ktor

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.request.ContentTransformationException
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.pipeline.PipelineContext
import jetbrains.buildServer.buildTriggers.remote.*
import jetbrains.buildServer.buildTriggers.remote.jackson.TYPE_VALIDATOR
import java.nio.file.Path
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

    private fun createServer() = embeddedServer(Netty, host = myHost, port = myPort) {
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
    KtorServer("127.0.0.1", 8080, TriggerPolicyManagerImpl(Path.of("triggers")))
}