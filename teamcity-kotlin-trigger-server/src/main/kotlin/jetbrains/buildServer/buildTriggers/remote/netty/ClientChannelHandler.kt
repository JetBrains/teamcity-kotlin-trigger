package jetbrains.buildServer.buildTriggers.remote.netty

import com.intellij.openapi.diagnostic.Logger
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.AttributeKey
import io.netty.util.ReferenceCountUtil
import jetbrains.buildServer.buildTriggers.remote.Constants
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

private const val MAX_QUEUE_SIZE = 100

internal val setActionsAttributeKey = AttributeKey.newInstance<(
        (Event) -> Unit,
        (Long, TimeUnit) -> Boolean?,
        () -> Unit
) -> Unit>("setActions")

internal class ClientChannelHandler : ChannelInboundHandlerAdapter() {
    private val myLogger = Logger.getInstance(ClientChannelHandler::class.qualifiedName)
    private val myAnswerQueue = LinkedBlockingQueue<Boolean>(MAX_QUEUE_SIZE)

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.channel().attr(setActionsAttributeKey).get()
            .invoke({ ctx.fireUserEventTriggered(it) },
                myAnswerQueue::poll,
                { ctx.close() })
    }

    // TODO: consider introducing time limit for put() operation or some other handling of queue size overflow
    @Suppress("UNCHECKED_CAST")
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        try {
            val answerMap = msg as Map<String, String>

            if (answerMap.containsKey(Constants.Response.ERROR))
                myLogger.warn(
                    ctx.channel()
                        .createLogMessage("Server responded with an error: ${answerMap[Constants.Response.ERROR]}")
                )

            val answer = answerMap[Constants.Response.ANSWER]
            myAnswerQueue.put(answer?.toBoolean() ?: false)
        } finally {
            ReferenceCountUtil.release(msg)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        myLogger.error(
            ctx.channel().createLogMessage("Inbound exception caught, connection will be closed"),
            cause
        )
        ctx.close()
    }

    private fun Channel.createLogMessage(msg: String) = "Channel #${id()}: $msg"
}
