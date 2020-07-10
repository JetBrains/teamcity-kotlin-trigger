package com.jetbrains.teamcity.boris.simplePlugin.netty

import com.google.gson.Gson
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

internal class ClientJsonEncoder : MessageToMessageEncoder<Map<String, String>>() {
    override fun encode(ctx: ChannelHandlerContext?, map: Map<String, String>?, out: MutableList<Any>?) {
        println("Client json encoder")
        out?.add(Gson().toJson(map)) ?: error("Could not produce encoded json: output list is null")
    }
}