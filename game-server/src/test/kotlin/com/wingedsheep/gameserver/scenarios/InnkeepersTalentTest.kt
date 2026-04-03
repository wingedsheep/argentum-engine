package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
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
