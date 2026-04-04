package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class InnkeepersTalentTest : ScenarioTestBase() {

    init {
        context("Innkeeper's Talent Level 1 — beginning of combat trigger") {
            test("puts a +1/+1 counter on target creature you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Innkeeper's Talent")
                    .withCardOnBattlefield(1, "Hired Claw", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Advance through phases — pass for both players repeatedly
                // until we get past BEGIN_COMBAT or get a pending decision
                var iterations = 0
                while (iterations < 20) {
                    if (game.state.pendingDecision != null) {
                        // If there's a target selection decision, handle it
                        val hiredClawId = game.findPermanent("Hired Claw")!!
                        game.selectTargets(listOf(hiredClawId))
                        continue
                    }
                    if (game.state.step == Step.DECLARE_ATTACKERS) break // Passed BEGIN_COMBAT
                    game.passPriority()
                    iterations++
                }

                // Resolve anything remaining on stack
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                val hiredClawId = game.findPermanent("Hired Claw")!!
                val counters = game.state.getEntity(hiredClawId)?.get<CountersComponent>()
                withClue("Hired Claw should have 1 +1/+1 counter (after ${iterations} iterations, phase=${game.state.phase}, step=${game.state.step})") {
                    counters shouldNotBe null
                    counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
            }
        }

        context("Innkeeper's Talent Level 2 — ward for permanents with counters") {
            test("permanents you control with counters have ward") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Innkeeper's Talent", classLevel = 2)
                    .withCardOnBattlefield(1, "Hired Claw", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hiredClawId = game.findPermanent("Hired Claw")!!

                // Advance to combat to trigger the Level 1 ability (put +1/+1 counter)
                var iterations = 0
                while (iterations < 20) {
                    if (game.state.pendingDecision != null) {
                        game.selectTargets(listOf(hiredClawId))
                        continue
                    }
                    if (game.state.step == Step.DECLARE_ATTACKERS) break
                    game.passPriority()
                    iterations++
                }

                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                // Hired Claw now has a +1/+1 counter
                val counters = game.state.getEntity(hiredClawId)?.get<CountersComponent>()
                withClue("Hired Claw should have a +1/+1 counter") {
                    counters shouldNotBe null
                    counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }

                // Check that Hired Claw now has ward keyword via projected state
                val projected = game.state.projectedState
                withClue("Hired Claw with a counter should have ward from Level 2") {
                    projected.hasKeyword(hiredClawId, Keyword.WARD) shouldBe true
                }

                // Innkeeper's Talent itself should NOT have ward (no counters on it)
                val talentId = game.findPermanent("Innkeeper's Talent")!!
                withClue("Innkeeper's Talent without counters should not have ward") {
                    projected.hasKeyword(talentId, Keyword.WARD) shouldBe false
                }
            }
        }

        context("Innkeeper's Talent Level 2 — ward enforcement") {
            test("ward counters spell when opponent can't pay") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Innkeeper's Talent", classLevel = 2)
                    .withCardOnBattlefield(1, "Hired Claw", summoningSickness = false)
                    .withCardInHand(2, "Carbonize")
                    .withLandsOnBattlefield(2, "Mountain", 3) // Exactly enough for Carbonize, nothing left for ward
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Manually add a +1/+1 counter to Hired Claw so ward is active
                val hiredClawId = game.findPermanent("Hired Claw")!!
                game.state = game.state.updateEntity(hiredClawId) { c ->
                    val counters = CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1)
                    c.with(counters)
                }

                // Verify ward is active
                game.state.projectedState.hasKeyword(hiredClawId, Keyword.WARD) shouldBe true

                // Opponent casts Carbonize targeting Hired Claw (taps all 3 Mountains)
                val castResult = game.castSpell(2, "Carbonize", hiredClawId)
                withClue("Should cast Carbonize: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Ward trigger goes on the stack; resolve it.
                // Opponent can't pay {1} (no untapped lands), so ward auto-counters Carbonize.
                game.resolveStack()

                // Carbonize was countered — Hired Claw should still be on battlefield
                withClue("Hired Claw should still be on the battlefield (Carbonize was countered by ward)") {
                    game.findPermanent("Hired Claw") shouldNotBe null
                }
            }

            test("ward counters spell when opponent chooses not to pay") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Innkeeper's Talent", classLevel = 2)
                    .withCardOnBattlefield(1, "Hired Claw", summoningSickness = false)
                    .withCardInHand(2, "Carbonize")
                    .withLandsOnBattlefield(2, "Mountain", 4) // 3 for Carbonize + 1 for ward, but opponent declines
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Manually add a +1/+1 counter to Hired Claw so ward is active
                val hiredClawId = game.findPermanent("Hired Claw")!!
                game.state = game.state.updateEntity(hiredClawId) { c ->
                    val counters = CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1)
                    c.with(counters)
                }

                // Opponent casts Carbonize targeting Hired Claw
                val castResult = game.castSpell(2, "Carbonize", hiredClawId)
                withClue("Should cast Carbonize: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Ward trigger resolves → "pay {1}?" decision
                game.resolveStack()
                withClue("Should have a pending decision for ward payment") {
                    game.state.pendingDecision shouldNotBe null
                }

                // Opponent chooses not to pay
                game.answerYesNo(false)

                // Carbonize should have been countered
                game.resolveStack()

                withClue("Hired Claw should still be on the battlefield (Carbonize was countered by ward)") {
                    game.findPermanent("Hired Claw") shouldNotBe null
                }
            }

            test("ward allows spell when opponent pays") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Innkeeper's Talent", classLevel = 2)
                    .withCardOnBattlefield(1, "Hired Claw", summoningSickness = false)
                    .withCardInHand(2, "Carbonize")
                    .withLandsOnBattlefield(2, "Mountain", 4) // 4 mountains: 3 for Carbonize + 1 for ward
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Manually add a +1/+1 counter to Hired Claw so ward is active
                val hiredClawId = game.findPermanent("Hired Claw")!!
                game.state = game.state.updateEntity(hiredClawId) { c ->
                    val counters = CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1)
                    c.with(counters)
                }

                // Opponent casts Carbonize targeting Hired Claw
                val castResult = game.castSpell(2, "Carbonize", hiredClawId)
                withClue("Should cast Carbonize: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Ward trigger resolves → "pay {1}?" decision
                game.resolveStack()
                withClue("Should have a pending decision for ward payment") {
                    game.state.pendingDecision shouldNotBe null
                }

                // Opponent pays {1}
                game.answerYesNo(true)

                // Carbonize resolves — Hired Claw takes 3 damage and should die
                game.resolveStack()

                withClue("Hired Claw should have been destroyed by Carbonize (opponent paid ward)") {
                    game.findPermanent("Hired Claw") shouldBe null
                }
            }
        }

        context("Innkeeper's Talent Level 3 — double counter placement") {
            test("doubling counter placement at level 3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Innkeeper's Talent", classLevel = 3)
                    .withCardOnBattlefield(1, "Hired Claw", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val talentId = game.findPermanent("Innkeeper's Talent")!!
                game.state.getEntity(talentId)?.get<ClassLevelComponent>()?.currentLevel shouldBe 3

                // Advance through phases
                var iterations = 0
                while (iterations < 20) {
                    if (game.state.pendingDecision != null) {
                        val hiredClawId = game.findPermanent("Hired Claw")!!
                        game.selectTargets(listOf(hiredClawId))
                        continue
                    }
                    if (game.state.step == Step.DECLARE_ATTACKERS) break
                    game.passPriority()
                    iterations++
                }

                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                val hiredClawId = game.findPermanent("Hired Claw")!!
                val counters = game.state.getEntity(hiredClawId)?.get<CountersComponent>()
                withClue("Hired Claw should have 2 +1/+1 counters (doubled by Level 3), phase=${game.state.phase}, step=${game.state.step}") {
                    counters shouldNotBe null
                    counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }
            }
        }
    }
}
