package jetbrains.buildServer.buildTriggers.remote

object Constants {
    object Request {
        const val ENABLE = "enable"
        const val DELAY = "delay"
        const val PREVIOUS_CALL_TIME = "previousCallTime"
        const val CURRENT_TIME = "currentTime"
    }
    object Response {
        const val ANSWER = "answer"
        const val ERROR = "error"
    }
}