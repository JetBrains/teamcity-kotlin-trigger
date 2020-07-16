package jetbrains.buildServer.buildTriggers.remote.ktor

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import jetbrains.buildServer.buildTriggers.remote.Constants
import jetbrains.buildServer.buildTriggers.remote.Trigger
import jetbrains.buildServer.buildTriggers.remote.TriggerLoader
import java.util.logging.Level
import java.util.logging.Logger

class KtorServer(private val myHost: String, private val myPort: Int) {
    private val myLogger = Logger.getLogger(KtorServer::class.qualifiedName)
    private val server = createServer()
    private val myTrigger: Trigger = TriggerLoader.loadTrigger()

    init {
        server.start(wait = true)
    }

    private fun createServer() = embeddedServer(Netty, host = myHost, port = myPort) {
        install(ContentNegotiation) {
            gson { }
        }
        routing {
            post("/") {
                val requestMap = call.receive<Map<String, String>>()
                val response = mutableMapOf<String, String>()

                try {
                    val answer = myTrigger.triggerBuild(requestMap)
                    response[Constants.Response.ANSWER] = answer.toString()
                    myLogger.info("Sending response: $answer")
                } catch (e: Exception) {
                    val msg = "Trigger ${myTrigger::class.qualifiedName} caused an exception: \"$e\""
                    myLogger.log(Level.SEVERE, msg, e)
                    response[Constants.Response.ERROR] = msg
                }

                call.respond(response)
            }
        }
    }
}

fun main() {
    KtorServer("127.0.0.1", 8080)
}