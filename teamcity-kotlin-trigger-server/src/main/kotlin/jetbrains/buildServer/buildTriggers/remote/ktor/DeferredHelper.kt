package jetbrains.buildServer.buildTriggers.remote.ktor

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

internal class DeferredHelper<T> {
    private val myDeferredValueMap = mutableMapOf<String, Deferred<T>>()

    // All exceptions thrown inside block will be visible by the caller
    fun tryComplete(id: String, block: suspend () -> T): T {
        val deferred = runBlocking {
            myDeferredValueMap.computeIfAbsent(id) {
                // GlobalScope is needed for exceptions to be preserved until we call await
                GlobalScope.async { block() }
            }
        }

        // no runBlocking here, because the exception will cancel the deferred
        if (!deferred.isCompleted)
            throw ValueNotCompleteException(id)

        @Suppress("DeferredResultUnused")
        myDeferredValueMap.remove(id)

        return runBlocking {
            deferred.await()
        }
    }
}

internal class ValueNotCompleteException(id: String) : RuntimeException("Deferred value not yet complete for id '$id'")