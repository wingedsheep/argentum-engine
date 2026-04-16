package com.wingedsheep.gym.service

import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit

/**
 * Thread pool for running per-environment operations in parallel.
 *
 * Each [com.wingedsheep.gym.GameEnvironment] is single-threaded —
 * `reset` / `step` on a given env must not race with itself. But different
 * envs are independent, so N envs can run in N threads. The pool schedules
 * per-env tasks; callers of [MultiEnvService.stepBatch] fan out through here.
 *
 * Uses [ForkJoinPool] rather than a fixed thread pool so nested work (future
 * MCTS rollouts that internally fork envs) doesn't starve.
 */
class EnvWorkerPool(
    parallelism: Int = Runtime.getRuntime().availableProcessors()
) {
    private val pool = ForkJoinPool(parallelism)

    /** Submit independent tasks and wait for all of them, preserving order. */
    fun <T> invokeAll(tasks: List<Callable<T>>): List<T> {
        if (tasks.isEmpty()) return emptyList()
        if (tasks.size == 1) return listOf(tasks.single().call())
        val futures = tasks.map { pool.submit(it) }
        return futures.map { it.get() }
    }

    /** Shut the pool down gracefully; awaits in-flight tasks. */
    fun close(awaitSeconds: Long = 5) {
        pool.shutdown()
        pool.awaitTermination(awaitSeconds, TimeUnit.SECONDS)
    }

    val parallelism: Int get() = pool.parallelism
}
