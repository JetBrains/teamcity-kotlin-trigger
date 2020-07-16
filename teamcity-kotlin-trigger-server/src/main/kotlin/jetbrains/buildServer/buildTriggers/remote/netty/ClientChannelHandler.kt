package jetbrains.buildServer.buildTriggers.remote.netty

import com.intellij.openapi.diagnostic.Logger
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.AttributeKey
import io.netty.util.ReferenceCountUtil
import jetbrains.buildServer.buildTriggers.remote.AwaitReadAction
import jetbrains.buildServer.buildTriggers.remote.CloseConnectionAction
import jetbrains.buildServer.buildTriggers.remote.Constants
import jetbrains.buildServer.buildTriggers.remote.FireEventAction

internal val setActionsAttributeKey = AttributeKey.newInstance<(
    FireEventAction,
    AwaitReadAction,
    CloseConnectionAction
) -> Unit>("setActions")

internal class ClientChannelHandler : ChannelInboundHandlerAdapter() {
    private val myLogger = Logger.getInstance(ClientChannelHandler::class.qualifiedName)
    private val myAnswerByTriggerIdMap = mutableMapOf<String, BlockingValueHolder<Boolean>>()

    override fun channelActive(ctx: ChannelHandlerContext) {
        val awaitReadAction: AwaitReadAction = { id, timeoutDuration, timeUnit ->
            synchronized(myAnswerByTriggerIdMap) {
                answerHolderByTriggerId(id).getAndRemove(timeoutDuration, timeUnit)
            }
        }

        ctx.channel()
            .attr(setActionsAttributeKey)
            .get()
            .invoke(
                { ctx.fireUserEventTriggered(it) },
                awaitReadAction,
                { ctx.close() }
            )
    }

    @Suppress("UNCHECKED_CAST")
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        try {
            val response = msg as Map<String, String>

            if (response.containsKey(Constants.Response.ERROR))
                myLogger.warn(
                    ctx.channel()
                        .createLogMessage("Server responded with an error: ${response[Constants.Response.ERROR]}")
                )

            val answer = response[Constants.Response.ANSWER]
            val triggerId = response[Constants.TRIGGER_ID] ?: run {
                myLogger.error(
                    ctx.channel().createLogMessage("Server response does not contain target trigger id")
                )
                return
            }

            answerHolderByTriggerId(triggerId).set(answer?.toBoolean() ?: false)
        } finally {
            ReferenceCountUtil.release(msg)
        }
    }

    private fun answerHolderByTriggerId(id: String): BlockingValueHolder<Boolean> {
        val answerHolder = myAnswerByTriggerIdMap[id] ?: BlockingValueHolder()
        if (!myAnswerByTriggerIdMap.containsKey(id))
            myAnswerByTriggerIdMap[id] = answerHolder
        return answerHolder
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
