package jetbrains.buildServer.buildTriggers.remote

import java.net.URLClassLoader

internal const val triggerClass = "jetbrains.buildServer.buildTriggers.remote.compiled.ScheduleTrigger"
internal val triggerPath = ClassLoader.getSystemResource("ScheduleTrigger.jar")

object TriggerLoader {
    internal fun loadTrigger(): Trigger {
        val urlClassLoader = URLClassLoader(arrayOf(triggerPath))
        val triggerClass = Class.forName(triggerClass, true, urlClassLoader)

        if (!Trigger::class.java.isAssignableFrom(triggerClass))
            throw ClassCastException("$triggerClass cannot be cast to ${Trigger::class.qualifiedName}")

        val instance = triggerClass.getDeclaredConstructor().newInstance()
        return instance as Trigger
    }
}