package com.wingedsheep.ai.arena

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * **The safety net for every search this plan adds after Phase 4.**
 *
 * The same agent, differing in nothing but the size of its `DecisionBudget`, played against itself
 * at 100 / 1000 / 3000 ms. Strength must be **monotone in the budget**. If it isn't, the search is
 * generating noise rather than signal, and the right response is to fix the leaf evaluator (Phases
 * 6 and 9) before stacking more samples on top of it — not to add rollouts (Phase 7).
 *
 * Built now, before rollouts exist, precisely so it is already calibrated when they land.
 *
 * ```
 * just arena-budget-scaling 300
 * ```
 *
 * The reported number is the **paired win share** of the bigger budget over the smaller, so 0.5 is
 * parity and the monotonicity claim is "every ladder rung is at or above 0.5". At the small game
 * counts this is normally run at, read the interval, not the point estimate: three runs of 300
 * games have interval half-widths around ±6%, so a rung at 0.48 is not a violation and a run that
 * puts a whole interval *below* parity is.
 *
 * A structural note that matters for reading the numbers: `SearchAllowances` converts a budget into
 * a **count of simulations**, not a stopwatch, so a rung is reproducible and a busy machine cannot
 * change the answer. See that class for why.
 */
class ArenaBudgetScalingTest : FunSpec({

    val enabled = System.getProperty("arenaBudgetScaling") == "true"
    val games = System.getProperty("arenaGames")?.toIntOrNull() ?: 300
    val seed = System.getProperty("arenaSeed")?.toLongOrNull() ?: ArenaConfig.DEFAULT_SEED
    val setCode = System.getProperty("arenaSet") ?: "BLB"
    val threads = System.getProperty("arenaThreads")?.toIntOrNull()
        ?: Runtime.getRuntime().availableProcessors()

    /**
     * The ladder, smallest budget first. Each rung is played as `bigger vs smaller`, so a healthy
     * result is a win share at or above 0.5 on every rung.
     */
    val ladder = listOf("v0-budget-100", "v0-budget-1000", "v0-budget-3000")

    test("budget scaling: strength is monotone in the decision budget").config(enabled = enabled) {
        println("=== BUDGET SCALING: ${ladder.joinToString(" < ")} — $games games per rung, seed $seed ===")
        println("    Each rung is `bigger vs smaller`; parity is 50%. A rung whose whole interval")
        println("    sits below 50% means more search made the agent worse.")
        println()

        val rungs = ladder.zipWithNext().map { (smaller, bigger) ->
            val config = ArenaConfig(
                agentA = ArenaAgents.resolve(bigger),
                agentB = ArenaAgents.resolve(smaller),
                games = games, seed = seed, setCode = setCode, threads = threads,
            )
            println("--- $bigger vs $smaller ---")
            Arena.run(config).also { print(ArenaReport.summary(it)) }
        }

        // The end-to-end rung too: the extremes should separate more than any single step does.
        val endToEnd = Arena.run(
            ArenaConfig(
                agentA = ArenaAgents.resolve(ladder.last()),
                agentB = ArenaAgents.resolve(ladder.first()),
                games = games, seed = seed, setCode = setCode, threads = threads,
            )
        )
        println("--- ${ladder.last()} vs ${ladder.first()} (end to end) ---")
        print(ArenaReport.summary(endToEnd))

        println()
        println("=== MONOTONICITY ===")
        (rungs + endToEnd).forEach { run ->
            val stats = run.stats
            val verdict = if (stats.pairWinShareCi.high < 0.5) "VIOLATION" else "ok"
            println(
                "  ${stats.agentA} vs ${stats.agentB}: ${pct(stats.pairWinShare)} " +
                    "CI [${pct(stats.pairWinShareCi.low)}, ${pct(stats.pairWinShareCi.high)}] — $verdict"
            )
        }

        // Only a *whole interval* below parity is a failure. A point estimate under 50% at 300
        // games is inside the noise, and failing on it would train everyone to ignore this test.
        val violations = (rungs + endToEnd)
            .filter { it.stats.pairWinShareCi.high < 0.5 }
            .map { "${it.stats.agentA} loses to ${it.stats.agentB} (${pct(it.stats.pairWinShare)})" }
        violations shouldBe emptyList()
    }

    /**
     * The always-on half: the ladder is real, its rungs are ordered, and the agents differ in
     * nothing but the budget. Cheap enough to run on every `:ai:test`, and it is what stops the
     * benchmark above from silently measuring two identical agents.
     */
    test("budget ladder agents differ only in budget size, in increasing order") {
        val profiles = ladder.map { ArenaAgents.resolve(it).profile }
        profiles.map { it.copy(id = "", budgetPolicy = profiles.first().budgetPolicy) }
            .distinct().size shouldBe 1
        profiles.map { it.budgetPolicy.toString() } shouldBe
            listOf("tiered(100ms)", "tiered(1000ms)", "tiered(3000ms)")
    }
})
