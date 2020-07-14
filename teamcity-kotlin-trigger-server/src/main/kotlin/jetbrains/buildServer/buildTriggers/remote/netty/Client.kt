package jetbrains.buildServer.buildTriggers.remote.netty

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.Mutex
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import jetbrains.buildServer.buildTriggers.remote.Actions
import kotlin.concurrent.thread

internal class Client(private val myHost: String, private val myPort: Int) {
    private val myLogger = Logger.getInstance(Client::class.qualifiedName)

    fun run(): Actions? {
        var actions: Actions? = null
        val actionsAvailable = Mutex()
        actionsAvailable.acquire() // when called later, make it block until actions are set

        thread {
            val workerGroup = NioEventLoopGroup()
            val channelFuture: ChannelFuture
            val bootstrap = Bootstrap()
                .group(workerGroup)
                .channel(NioSocketChannel::class.java)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(channel: SocketChannel) {
                        channel.pipeline()
                            .addLast(stringCodec())
                            .addLast(JsonCodec())
                            .addLast(ClientChannelHandler())
                            .addLast(TriggerBuildEventHandler())

                        channel.attr(setActionsAttributeKey)
                            .set { fireEvent, awaitRead, closeConnection ->
                                actions = Actions(fireEvent, awaitRead, closeConnection)
                                actionsAvailable.release()
                            }
                    }
                })
            try {
                channelFuture = bootstrap.connect(myHost, myPort).sync()
                channelFuture.channel().closeFuture().sync() // wait until connection is closed
                actions?.outdated = true
                myLogger.debug("Connection shut down without exception")
            } catch (e: Exception) {
                workerGroup.shutdownGracefully()
                myLogger.error("Exception caught on connection to $myHost:$myPort", e)

                actions?.outdated = true
                actionsAvailable.release()
            }
        }
        actionsAvailable.acquire()
        return actions
    }
}
