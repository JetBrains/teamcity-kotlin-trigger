package jetbrains.buildServer.buildTriggers.remote.netty

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

internal class TriggerBuildEventHandler : ChannelInboundHandlerAdapter() {
    @Suppress("NAME_SHADOWING")
    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any?) {
        when (evt) {
            is TriggerBuild -> ctx.writeAndFlush(evt.myParams)
        }
    }
}