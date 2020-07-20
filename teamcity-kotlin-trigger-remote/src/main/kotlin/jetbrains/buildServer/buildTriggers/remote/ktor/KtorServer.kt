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
import jetbrains.buildServer.buildTriggers.remote.jackson.TypeValidator
import java.util.logging.Level
import java.util.logging.Logger

class KtorServer(private val myHost: String, private val myPort: Int) {
    private val myLogger = Logger.getLogger(KtorServer::class.qualifiedName)
    private val server = createServer()

    init {
        server.start(wait = true)
    }

    private fun Routing.handleRoute(
        mapping: Mapping,
        block: suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit
    ) = route(mapping.path, mapping.httpMethod) {
        handle(block)
    }

    private fun createServer() = embeddedServer(Netty, host = myHost, port = myPort) {
        install(ContentNegotiation) {
            jackson {
                activateDefaultTyping(TypeValidator.myTypeValidator)
            }
        }
        routing {
            handleRoute(RequestMapping.triggerBuild("{triggerName}")) {
                defaultServerRespond {
                    val request = try {
                        call.receive<TriggerBuildRequest>()
                    } catch (e: ContentTransformationException) {
                        throw ContentTypeMismatch(e)
                    }

                    val triggerName = call.parameters["triggerName"] ?: throw NoTriggerNameError()

                    val myTrigger = try {
                        TriggerManager.loadTrigger(triggerName)
                    } catch (e: TriggerManager.TriggerDoesNotExistException) {
                        throw TriggerDoesNotExistError(triggerName)
                    } catch (e: Exception) {
                        throw TriggerLoadingError(e)
                    }

                    val answer = try {
                        myTrigger.triggerBuild(request)
                    } catch (e: Exception) {
                        throw InternalTriggerError(e)
                    }

                    myLogger.info("Sending response: $answer")
                    TriggerBuildResponse(answer)
                }
            }

            handleRoute(RequestMapping.uploadTrigger("{triggerName}")) {
                defaultServerRespond {
                    val triggerBytes = try {
                        call.receive<UploadTriggerRequest>().triggerBody
                    } catch (e: ContentTransformationException) {
                        throw ContentTypeMismatch(e)
                    }

                    val triggerName = call.parameters["triggerName"] ?: throw NoTriggerNameError()

                    TriggerManager.saveTrigger(triggerName, triggerBytes)
                    myLogger.info("Trigger $triggerName loaded")
                    UploadTriggerResponse
                }
            }
        }
    }

    private suspend fun <T : Any> PipelineContext<Unit, ApplicationCall>.defaultServerRespond(block: suspend () -> T) {
        try {
            call.respond(block())
        } catch (se: ServerError) {
            myLogger.severe(se.message)
            call.respond(se.asResponse())
        } catch (e: Exception) {
            myLogger.log(Level.SEVERE, "Unknown internal server error", e)
            call.respond(InternalServerError(e).asResponse())
        }
    }
}

fun main() {
    KtorServer("127.0.0.1", 8080)
}