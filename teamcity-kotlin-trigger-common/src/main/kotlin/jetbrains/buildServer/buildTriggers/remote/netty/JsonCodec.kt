package jetbrains.buildServer.buildTriggers.remote.netty

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec

class JsonCodec : MessageToMessageCodec<String, Map<String, String>>() {
    override fun encode(ctx: ChannelHandlerContext, map: Map<String, String>, out: MutableList<Any>) {
        out.add(Gson().toJson(map))
    }

    override fun decode(ctx: ChannelHandlerContext, str: String, out: MutableList<Any>) {
        out.add(Gson().fromJson<Map<String, String>>(str))
    }
}