package de.itemis.mps.gradle.modelcheck

import org.jetbrains.mps.openapi.util.Consumer
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A thread-safe collector for items. Items are collected into thread-local lists and then flattened into a single list
 * when `result()` is called. This avoids contention on a single list when multiple threads are adding items
 * concurrently.
 */
class ThreadSafeCollector<T : Any> : Consumer<T> {
    private val allLists = ConcurrentLinkedQueue<MutableList<T>>()

    private val local = ThreadLocal.withInitial {
        mutableListOf<T>().also(allLists::add)
    }

    override fun consume(item: T) {
        local.get().add(item)
    }

    fun result(): List<T> = allLists.flatten()
}
