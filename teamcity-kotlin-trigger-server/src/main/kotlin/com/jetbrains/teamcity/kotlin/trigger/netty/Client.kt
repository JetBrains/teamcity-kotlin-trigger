package com.jetbrains.teamcity.kotlin.trigger.netty

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.Mutex
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.string.StringEncoder
import io.netty.util.CharsetUtil
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

internal class Client(private val host: String, private val port: Int) {
    fun run(): Actions {
        val workerGroup = NioEventLoopGroup()

        var actions: Actions? = null
        val actionsAvailable = Mutex()
        actionsAvailable.acquire() // when called later, make it block until actions are set

        val channelFuture: ChannelFuture
        try {
            val bootstrap = Bootstrap()
                    .group(workerGroup)
                    .channel(NioSocketChannel::class.java)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(channel: SocketChannel?) {
                            channel?.also {
                                channel.pipeline()
                                        .addLast(StringEncoder(CharsetUtil.UTF_8))
                                        .addLast(ClientJsonEncoder())
                                        .addLast(ClientHandler())
                                        .addLast(TriggerBuildEventHandler())

                                channel.attr(setActionsAttributeKey)
                                        .set { fireEvent, awaitRead, closeConnection ->
                                            actions = Actions(fireEvent, awaitRead, closeConnection)
                                            actionsAvailable.release()
                                        }
                            }
                        }
                    })
            channelFuture = bootstrap.connect(host, port).sync()
            actionsAvailable.acquire()
        } catch (e: Exception) {
            workerGroup.shutdownGracefully()
            throw e
        }

        thread {
            try {
                channelFuture.channel().closeFuture().sync() // wait until connection is closed
            } finally {
                workerGroup.shutdownGracefully()
                val logger = Logger.getInstance(Client::class.qualifiedName)
                if (logger.isDebugEnabled) {
                    logger.debug("Connection shut down without exception")
                }
            }
        }

        return actions!!
    }
}

internal class Actions(val fireEvent: (Event) -> Unit,
              val awaitRead: (Long, TimeUnit) -> Boolean?,
                              val closeConnection: () -> Unit)