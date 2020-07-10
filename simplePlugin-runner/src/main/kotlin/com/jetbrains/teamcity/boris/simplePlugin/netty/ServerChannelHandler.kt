package com.jetbrains.teamcity.boris.simplePlugin.netty

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.text.DateFormat
import java.time.Instant

class ServerHandler : ChannelInboundHandlerAdapter() {
    @Suppress("NAME_SHADOWING")
    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        ctx!!.also { ctx ->
            msg!!.also { msg ->
                val dataMap = msg as Map<*, *>

                val enableStr = dataMap[ENABLE] as? String ?: return
                val delayStr = dataMap[DELAY] as? String ?: return
                val prevCallTimeStr = dataMap[PREVIOUS_CALL_TIME] as? String
                val currTimeStr = dataMap[CURRENT_TIME] as? String ?: return

                println("Enable: $enableStr")
                println("Delay: $delayStr")
                println("Prev call time: $prevCallTimeStr")
                println("Curr time: $currTimeStr")

                val enable = enableStr.toBoolean()
                val delay = delayStr.toLongOrNull()
                val prevCallTime = prevCallTimeStr?.toLongOrNull()//if (prevCallTimeStr == "null") null else DateFormat
                // .getDateTimeInstance().parse(prevCallTimeStr)
                val currTime = currTimeStr.toLong()//DateFormat.getDateTimeInstance().parse(currTimeStr)

                val answer = if (!enable || null == delay) false
                else prevCallTime == null || currTime - prevCallTime > delay * 60_000
//                else currTime.toInstant().minusMillis(prevCallTime.time).isAfter(Instant.ofEpochSecond(delay * 60))

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

internal const val ENABLE = "enable"
internal const val DELAY = "delay"
internal const val PREVIOUS_CALL_TIME = "previousCallTime"
internal const val CURRENT_TIME = "currentTime"
