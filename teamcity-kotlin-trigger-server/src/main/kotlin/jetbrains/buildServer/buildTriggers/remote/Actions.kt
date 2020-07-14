package jetbrains.buildServer.buildTriggers.remote

import jetbrains.buildServer.buildTriggers.remote.netty.Event
import java.util.concurrent.TimeUnit

/**
 * Represents actions, specific to a particular connection.
 */
internal class Actions(
    val fireEvent: FireEventAction,
    val awaitRead: AwaitReadAction,
    closeConnection: CloseConnectionAction
) {
    @Volatile
    var outdated: Boolean = false

    val closeConnection: CloseConnectionAction = {
        synchronized(this) {
            closeConnection()
            outdated = true
        }
    }
}

internal typealias FireEventAction = (Event) -> Unit
internal typealias AwaitReadAction = (String, Long, TimeUnit) -> Boolean?
internal typealias CloseConnectionAction = () -> Unit