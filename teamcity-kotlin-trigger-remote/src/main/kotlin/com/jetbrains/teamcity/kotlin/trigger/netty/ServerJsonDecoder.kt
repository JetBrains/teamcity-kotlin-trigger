package com.jetbrains.teamcity.kotlin.trigger.netty

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder

class ServerJsonDecoder : MessageToMessageDecoder<String>() {
    @Suppress("NAME_SHADOWING")
    override fun decode(ctx: ChannelHandlerContext?, str: String?, out: MutableList<Any>?) {
        val typeToken = object : TypeToken<Map<String, String>>() {}.type
        val dataMap = Gson().fromJson<Map<String, String>>(str, typeToken)
        out?.add(dataMap) ?: error("Could not produce decoded dataMap: output list is null")
    }
}