package jetbrains.buildServer.buildTriggers.remote.netty

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.util.logging.Level
import java.util.logging.Logger

class ServerExceptionHandler(private val myLogger: Logger) : ChannelInboundHandlerAdapter() {
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        myLogger.log(
            Level.SEVERE,
            ctx.channel().createLogMessage("Inbound exception caught, connection will be closed"),
            cause
        )
        ctx.close()
    }
}