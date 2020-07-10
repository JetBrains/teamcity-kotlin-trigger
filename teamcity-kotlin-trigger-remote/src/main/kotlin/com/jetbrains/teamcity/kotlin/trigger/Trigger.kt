package com.jetbrains.teamcity.kotlin.trigger

internal interface Trigger {
    fun triggerBuild(context: Map<String, String>): Boolean
}

internal const val ENABLE = "enable"
internal const val DELAY = "delay"
internal const val PREVIOUS_CALL_TIME = "previousCallTime"
internal const val CURRENT_TIME = "currentTime"
