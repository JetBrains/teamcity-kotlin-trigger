package jetbrains.buildServer.buildTriggers.remote.netty

import io.netty.channel.CombinedChannelDuplexHandler
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.util.CharsetUtil
import java.nio.charset.Charset

fun stringCodec(charset: Charset = CharsetUtil.UTF_8) =
    CombinedChannelDuplexHandler(StringDecoder(charset), StringEncoder(charset))