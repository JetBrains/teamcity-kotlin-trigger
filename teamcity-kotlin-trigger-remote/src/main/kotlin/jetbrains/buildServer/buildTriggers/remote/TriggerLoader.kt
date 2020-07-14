package jetbrains.buildServer.buildTriggers.remote

internal const val triggerPath = "jetbrains.buildServer.buildTriggers.remote.compiled.TriggerImpl"

object TriggerLoader {
    internal fun loadTrigger(): Trigger {
        val triggerClass = Class.forName(triggerPath)

        if (!Trigger::class.java.isAssignableFrom(triggerClass))
            throw ClassCastException("$triggerPath cannot be cast to ${Trigger::class.qualifiedName}")

        val instance = triggerClass.getDeclaredConstructor().newInstance()
        return instance as Trigger
    }
}