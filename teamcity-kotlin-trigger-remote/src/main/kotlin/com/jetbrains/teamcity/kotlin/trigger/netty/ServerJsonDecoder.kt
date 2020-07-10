package com.jetbrains.teamcity.kotlin.trigger.netty

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder

class ServerJsonDecoder : MessageToMessageDecoder<String>() {
    override fun decode(ctx: ChannelHandlerContext, str: String, out: MutableList<Any>) {
        val dataMap = Gson().fromJson<Map<String, String>>(str)
        out.add(dataMap)
    }
}