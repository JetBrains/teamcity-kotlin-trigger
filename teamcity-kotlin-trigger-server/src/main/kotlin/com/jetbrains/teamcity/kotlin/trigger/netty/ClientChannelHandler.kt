package com.jetbrains.teamcity.kotlin.trigger.netty

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.AttributeKey
import io.netty.util.ReferenceCountUtil
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

internal const val MAX_QUEUE_SIZE = 100

internal val setActionsAttributeKey = AttributeKey.newInstance<(
        (Event) -> Unit,
        (Long, TimeUnit) -> Boolean?,
        () -> Unit
) -> Unit>("setActions")

internal class ClientHandler : ChannelInboundHandlerAdapter() {

    private val answerQueue = LinkedBlockingQueue<Boolean>(MAX_QUEUE_SIZE)

    @Suppress("NAME_SHADOWING")
    override fun channelActive(ctx: ChannelHandlerContext?) {
        ctx?.also { ctx ->
            ctx.channel().attr(setActionsAttributeKey).get()
                    .invoke({ ctx.fireUserEventTriggered(it) },
                            answerQueue::poll,
                            { ctx.close() })
        }
    }

    // TODO: consider introducing time limit for put() operation or some other handling of queue size overflow
    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        try {
            val answer = (msg as? ByteBuf)?.readBoolean() ?: return
            answerQueue.put(answer)
        } finally {
            ReferenceCountUtil.release(msg)
        }
    }
}
