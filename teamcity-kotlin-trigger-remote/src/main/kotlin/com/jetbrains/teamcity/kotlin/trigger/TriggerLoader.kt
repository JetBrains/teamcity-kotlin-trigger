package com.jetbrains.teamcity.kotlin.trigger

internal const val triggerPath = "com.jetbrains.teamcity.kotlin.trigger.compiled.TriggerImpl"

internal fun loadTrigger(): Trigger {
    val triggerClass = Class.forName(triggerPath)

    if (!Trigger::class.java.isAssignableFrom(triggerClass))
        throw ClassCastException("$triggerPath cannot be cast to ${Trigger::class.qualifiedName}")

    val instance = triggerClass.getDeclaredConstructor().newInstance()
    return instance as Trigger
}