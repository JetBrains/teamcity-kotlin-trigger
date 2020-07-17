package jetbrains.buildServer.buildTriggers.remote.ktor

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

internal class DeferredHelper<T> {
    private val deferredValueMap = mutableMapOf<String, Deferred<T>>()

    fun tryComplete(id: String, block: suspend () -> T): T = synchronized(this) {
        val deferred = runBlocking {
            deferredValueMap.computeIfAbsent(id) { async { block() } }
        }

        // no runBlocking here, because the exception will cancel the deferred
        if (!deferred.isCompleted)
            throw ValueNotCompleteException(id)

        @Suppress("DeferredResultUnused")
        deferredValueMap.remove(id)

        runBlocking {
            deferred.await()
        }
    }
}

internal class ValueNotCompleteException(id: String) : RuntimeException("Deferred value not yet complete for id '$id'")