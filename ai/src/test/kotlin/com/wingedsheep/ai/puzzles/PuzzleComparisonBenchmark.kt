package com.wingedsheep.ai.puzzles

import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.engine.rollout.RolloutSettings
import com.wingedsheep.engine.support.ScenarioTestBase

/**
 * The same 66 puzzles across several agents, side by side.
 *
 * ```
 * just arena-puzzles-compare
 * ```
 *
 * Disabled unless `-Dbenchmark=true`, so a normal `:ai:test` run only pays for [PuzzleSuiteTest].
 * The value over the plain suite is attribution: the `v0` / `production` columns say whether the
 * card advisors are earning anything on the tactics they were written for, and later phases add a
 * column each.
 */
class PuzzleComparisonBenchmark : ScenarioTestBase() {

    init {
        val enabled = System.getProperty("benchmark") == "true"

        test("puzzles: profile comparison").config(enabled = enabled) {
            val runner = PuzzleRunner(cardRegistry) { scenario() }
            val profiles = listOf(
                AiProfile.LEGACY_V0,
                AiProfile.PRODUCTION,
                // The promotion pair. `production` is what ships today and `production-candidate`
                // is what flipping Phases 4/7/8 on would ship, so this is the only column pair on
                // the suite that prices the decision actually in front of us.
                AiProfile.PRODUCTION_CANDIDATE,
                // The two cheap targeted fixes, with and without the rollouts on top. If
                // `production-tuned` matches `production-candidate-tuned` on the suite, the
                // rollouts are not what is closing these puzzles.
                AiProfile.PRODUCTION_TUNED,
                AiProfile.PRODUCTION_CANDIDATE_TUNED,
                // One variable each, so a puzzle that moves can be attributed.
                AiProfile.PRODUCTION_HORIZON,
                AiProfile.PRODUCTION_CONCAVE,
                AiProfile.PRODUCTION_CONCAVE_2,
                AiProfile.PRODUCTION_HORIZON_CONCAVE,
                AiProfile.PRODUCTION_HORIZON_CONCAVE_2,
                AiProfile.PRODUCTION_CRACKBACK,
                AiProfile.PRODUCTION_TARGETED,
                // The land-drop accounting alone, and on top of what is live. `sequencing-02` is the
                // one verdict that moves in either column, which is what makes it attributable.
                AiProfile.PRODUCTION_LANDDROP,
                AiProfile.PRODUCTION_CANDIDATE_LANDDROP,
                // Land order, alone and on top of what is live. `sequencing-07` is the verdict that
                // should move, with `sequencing-08` — the same board, the opposite answer — held.
                AiProfile.PRODUCTION_LANDSEQ,
                AiProfile.PRODUCTION_CANDIDATE_LANDSEQ,
                // The combat trick window, alone and on top of what is live. `instants-08` is the
                // verdict that should move, with `instants-07` — the same board one step later,
                // the opposite answer — held. Only the candidate column can move `instants-07`:
                // spending the trick needs the budget half, which needs tiers.
                AiProfile.PRODUCTION_TRICKWINDOW,
                AiProfile.PRODUCTION_CANDIDATE_TRICKWINDOW,
                // The race-clock bound, alone and on top of what is live. `lastchance-05` is the
                // verdict that should move. This one touches every position with an empty board,
                // so the column is read for what it *costs* as much as for what it closes.
                AiProfile.PRODUCTION_RACECLOCK,
                AiProfile.PRODUCTION_CANDIDATE_RACECLOCK,
                // Removal patience, alone and on top of what is live. `timing-01` and `removal-07`
                // are the verdicts that should move — but only in the candidate column, since the
                // turns-form race sentinel outscores any discount on `production`. `removal-08`
                // (the same board with a full hand) and `noncreature-01` (the Disenchant a constant
                // penalty used to veto) are the negative controls: both must stay passing.
                AiProfile.PRODUCTION_PATIENCE,
                AiProfile.PRODUCTION_CANDIDATE_PATIENCE,
                // The two creature-valuation corrections, each alone and both on top of what is
                // live. `activate-04` and `removal-03` are the verdicts that should move — the two
                // the race clock traded away — and only the candidate column can move `removal-03`,
                // since on `production` it passes for the sentinel's reason rather than the Wurm's.
                // `removal-01` / `-02` and `keywords` are the negative controls: raw creature value
                // still has to rank a 6/4 over a 2/2 and read a Wall as a Wall.
                AiProfile.PRODUCTION_DAMAGEFADES,
                AiProfile.PRODUCTION_PACIFIED,
                AiProfile.PRODUCTION_CANDIDATE_BOARDVALUE,
                // The cantrip end-step window, alone and on top of what is live. `timing-05` is the
                // verdict that should move, with `instants-06` — the same window, the opposite
                // answer — held. A column that moves both has broken the control.
                AiProfile.PRODUCTION_CANTRIP,
                AiProfile.PRODUCTION_CANDIDATE_CANTRIP,
                // Lands priced as mana, alone and on top of what is live. `sequencing-02` is the
                // verdict that should move on the isolation column — it is the puzzle the earmark
                // this model supersedes was built for.
                AiProfile.PRODUCTION_MANALANDS,
                AiProfile.PRODUCTION_CANDIDATE_MANALANDS,
                // Counterspell patience, alone and on top of what is live. `respond-02` is the
                // verdict that should move; the rest of the `respond` category is the negative
                // control, and all four of those are cast by a tapped-out opponent, where the bar
                // is zero by construction.
                AiProfile.PRODUCTION_COUNTERPATIENCE,
                AiProfile.PRODUCTION_CANDIDATE_COUNTERPATIENCE,
                // Flash creatures held for the ambush, alone and on top of what is live.
                // `instants-09` is the verdict that should move; `instants-10` is the ambush itself
                // and must not, and the three guards (`instants-11`, `-12`, `-13`) are the negative
                // controls for haste, an ETB that clears a blocker, and the late-game release.
                AiProfile.PRODUCTION_AMBUSH,
                AiProfile.PRODUCTION_CANDIDATE_AMBUSH,
                // Expiring grants held for a window that can spend them, alone and on top of what
                // is live. `instants-14` is the verdict that should move; `instants-15` (the
                // released window) and `instants-16` (a payoff that outlives the turn) are the
                // negative controls and must not.
                AiProfile.PRODUCTION_EXPIRING,
                AiProfile.PRODUCTION_CANDIDATE_EXPIRING,
                // Phase 7's rollout evaluator, isolated from Phases 4 and 6 so the column is
                // attributable to the rollouts alone.
                AiProfile.PHASE7,
                // The mixture's own controls: pure rollout at one end, today's static leaf at the
                // other. `staticWeight` is the one parameter Phase 7 could not derive in advance.
                AiProfile.PHASE7.copy(
                    id = "v0-rollout-pure",
                    rollouts = RolloutSettings.DEFAULT.copy(staticWeight = 0.0),
                ),
                AiProfile.PHASE7.copy(
                    id = "v0-rollout-25",
                    rollouts = RolloutSettings.DEFAULT.copy(staticWeight = 0.25),
                ),
                AiProfile.PHASE7.copy(
                    id = "v0-rollout-75",
                    rollouts = RolloutSettings.DEFAULT.copy(staticWeight = 0.75),
                ),
                // Phases 4 and 6 without the rollouts — the reference the full stack has to beat
                // for Phase 7 to be earning anything on top of what already shipped.
                AiProfile.PHASE4_PHASE6,
                // Everything the plan proposes to ship.
                AiProfile.PHASE4_PHASE6_PHASE7,
                // The discrimination control, same as the arena's: every weight zero.
                AiProfile.LEGACY_V0.copy(id = "v0-blind", evalWeightsId = "blind"),
            )

            val runs = profiles.map { profile ->
                val results = runner.runAll(PuzzleCatalog.all, profile)
                println(PuzzleReport.summary(profile.id, results))
                println()
                profile.id to results
            }

            println(PuzzleReport.comparison(runs))
        }
    }
}
