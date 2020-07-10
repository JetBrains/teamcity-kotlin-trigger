package com.jetbrains.teamcity.boris.simplePlugin.netty

import com.jetbrains.teamcity.boris.simplePlugin.Trigger
import com.jetbrains.teamcity.boris.simplePlugin.loadTrigger
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.util.logging.Level
import java.util.logging.Logger

internal class ServerChannelHandler : ChannelInboundHandlerAdapter() {
    private var trigger: Trigger? = null

    override fun channelActive(ctx: ChannelHandlerContext?) {
        if (trigger == null)
            trigger = loadTrigger()
    }

    @Suppress("NAME_SHADOWING", "UNCHECKED_CAST")
    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        ctx!!.also { ctx ->
            msg!!.also { msg ->
                val logger = Logger.getLogger(ServerChannelHandler::class.qualifiedName + "_" + ctx.channel().id())

                val dataMap = msg as Map<String, String>
                val answer = trigger?.triggerBuild(dataMap) ?: run {
                    logger.warning("Trigger is not loaded")
                    false
                }

                val response = ctx.alloc().buffer(1).writeBoolean(answer)
                ctx.writeAndFlush(response)
                logger.info("Sending response: $answer")
            }
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        ctx!!.also {
            val logger = Logger.getLogger(ServerChannelHandler::class.qualifiedName + "_" + ctx.channel().id())
            logger.log(Level.SEVERE, "Inbound exception caught, connection will be closed", cause)
            ctx.close()
        }
    }
}
