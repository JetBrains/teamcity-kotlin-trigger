package jetbrains.buildServer.buildTriggers.remote.ktor

import jetbrains.buildServer.buildTriggers.remote.*
import java.util.logging.Logger

internal class ActionHandler(
    private val myTriggerManager: TriggerManager, private val myLogger: Logger
) {
//    fun triggerActivated(triggerName: String, context: TriggerContext): OkayResponse {
//        val trigger = loadTrigger(triggerName)
//        try {
//            trigger.triggerActivated(context)
//        } catch (e: Throwable) {
//            myLogger.severe("Trigger $triggerName activation failed")
//            throw internalTriggerError(e)
//        }
//        myLogger.info("Trigger $triggerName activated")
//        return OkayResponse
//    }
//
//    fun triggerDeactivated(triggerName: String, context: TriggerContext): OkayResponse {
//        val trigger = loadTrigger(triggerName)
//        try {
//            trigger.triggerDeactivated(context)
//        } catch (e: Throwable) {
//            myLogger.severe("Trigger $triggerName deactivation failed")
//            throw internalTriggerError(e)
//        }
//        myLogger.info("Trigger $triggerName deactivated")
//        return OkayResponse
//    }

    fun triggerBuild(triggerName: String, context: TriggerContext): TriggerBuildResponse {
        val trigger = loadTrigger(triggerName)
        val answer = try {
            trigger.triggerBuild(context)
        } catch (e: Throwable) {
            myLogger.severe("Failed to call triggerBuild() on trigger $triggerName")
            throw internalTriggerError(e)
        }

        myLogger.info("Trigger $triggerName is sending response: $answer")
        return TriggerBuildResponse(answer, context.customData)
    }

    fun uploadTrigger(triggerName: String, triggerBody: TriggerBody): OkayResponse {
        myTriggerManager.saveTrigger(triggerName, triggerBody.bytes)
        myLogger.info("Trigger $triggerName loaded")
        return OkayResponse
    }

    private fun loadTrigger(triggerName: String): TriggerService =
        try {
            myTriggerManager.loadTrigger(triggerName)
        } catch (e: TriggerDoesNotExistException) {
            throw triggerDoesNotExistError(triggerName)
        } catch (e: Throwable) {
            throw triggerLoadingError(e)
        }
}
