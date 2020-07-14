package jetbrains.buildServer.buildTriggers.remote.netty

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import jetbrains.buildServer.buildTriggers.remote.Constants
import jetbrains.buildServer.buildTriggers.remote.Trigger
import jetbrains.buildServer.buildTriggers.remote.TriggerLoader
import java.util.logging.Logger

internal class ServerChannelHandler : ChannelInboundHandlerAdapter() {
    private val myLogger = Logger.getLogger(ServerChannelHandler::class.qualifiedName)
    private lateinit var myTrigger: Trigger

    override fun channelActive(ctx: ChannelHandlerContext) {
        if (!this::myTrigger.isInitialized)
            myTrigger = TriggerLoader.loadTrigger()
    }

    @Suppress("UNCHECKED_CAST")
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (!this::myTrigger.isInitialized) {
            val response = mapOf(Constants.Response.ERROR to "Server is in an improper state: trigger is not loaded")
            ctx.writeAndFlush(response).addListener {
                ctx.fireExceptionCaught(IllegalStateException("Trigger is not loaded"))
            }
            return
        }

        val dataMap = msg as Map<String, String>
        val answer = try {
            myTrigger.triggerBuild(dataMap)
        } catch (e: Exception) {
            val response =
                mapOf(Constants.Response.ERROR to "Trigger ${myTrigger::class.qualifiedName} caused an exception: \"$e\"")
            ctx.writeAndFlush(response).addListener {
                ctx.fireExceptionCaught(e)
            }
            return
        }

        val response = mapOf(Constants.Response.ANSWER to answer.toString())
        ctx.writeAndFlush(response)
        myLogger.info(ctx.channel().createLogMessage("Sending response: $answer"))
    }
}

internal fun Channel.createLogMessage(msg: String) = "Channel ${id()}: $msg"
