package com.jetbrains.teamcity.boris.simplePlugin.netty

import com.jetbrains.teamcity.boris.simplePlugin.Trigger
import com.jetbrains.teamcity.boris.simplePlugin.loadTrigger
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

internal class ServerHandler : ChannelInboundHandlerAdapter() {
    private var trigger: Trigger? = null

    override fun channelActive(ctx: ChannelHandlerContext?) {
        if (trigger == null)
            trigger = loadTrigger()
    }

    @Suppress("NAME_SHADOWING", "UNCHECKED_CAST")
    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        ctx!!.also { ctx ->
            msg!!.also { msg ->
                val dataMap = msg as Map<String, String>

                val answer = trigger?.triggerBuild(dataMap) ?: run {
                    println("Trigger is not loaded")
                    false
                }

                val response = ctx.alloc().buffer(1).writeBoolean(answer)
                ctx.writeAndFlush(response)
                println("Answer: $answer")
                println()
            }
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        cause?.printStackTrace()
        ctx?.close()
    }
}
