package com.jetbrains.teamcity.kotlin.trigger.netty

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

internal class TriggerBuildEventHandler : ChannelInboundHandlerAdapter() {
    @Suppress("NAME_SHADOWING")
    override fun userEventTriggered(ctx: ChannelHandlerContext?, evt: Any?) {
        ctx?.also { ctx ->
            when (evt) {
                is TriggerBuild -> {
                    ctx.writeAndFlush(evt.params)
                }
            }
        }
    }


}