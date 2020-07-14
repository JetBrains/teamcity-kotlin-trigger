package jetbrains.buildServer.buildTriggers.remote.netty

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Represents a thread-safe atomic container, allowing subscribing to the appearance of the value
 */
internal class BlockingValueHolder<T> {
    private val myBlockingQueue = LinkedBlockingQueue<T>(1)

    fun getAndRemove(timeout: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): T? = synchronized(this) {
        myBlockingQueue.poll(timeout, timeUnit)
    }

    /**
     * @return Previous value if it was present, new value if not
     */
    fun set(value: T): T = synchronized(this) {
        val rv =
            if (myBlockingQueue.isNotEmpty())
                myBlockingQueue.remove()
            else value

        myBlockingQueue.put(value)
        rv
    }
}
