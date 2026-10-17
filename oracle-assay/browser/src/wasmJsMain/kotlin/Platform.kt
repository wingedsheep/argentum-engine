package com.wingedsheep.assay.browser

// Each checker worker is single-threaded. Preserve nested scopes without JVM thread state.
internal class ThreadLocal<T>(initial: () -> T? = { null }) {
    private var value: T? = initial()
    @Suppress("UNCHECKED_CAST")
    fun get(): T = value as T
    fun set(next: T) { value = next }
    companion object {
        fun <T> withInitial(initial: () -> T): ThreadLocal<T> = ThreadLocal(initial)
    }
}
internal class AtomicInteger(private var value: Int) {
    fun getAndIncrement(): Int = value++
}
internal object UUID {
    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    fun randomUUID(): kotlin.uuid.Uuid = kotlin.uuid.Uuid.random()
}
