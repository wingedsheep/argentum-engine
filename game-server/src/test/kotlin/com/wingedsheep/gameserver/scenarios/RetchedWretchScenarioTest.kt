package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Retched Wretch (Lorwyn Eclipsed).
 *
 * Card reference:
 * - Retched Wretch ({2}{B}) — 4/2 Creature — Goblin
 *   When this creature dies, if it had a -1/-1 counter on it, return it to the
 *   battlefield under its owner's control and it loses all abilities.
 *
 * The trigger fires only when the dying creature had a -1/-1 counter on it
 * (intervening-if). The new TriggeringEntityHadMinusOneMinusOneCounter condition
 * is what gates the trigger; we test both the trigger-fires path and the
 * trigger-skipped path.
 */
class RetchedWretchScenarioTest : ScenarioTestBase() {

    init {
        context("Retched Wretch dies trigger") {

            test("dies with a -1/-1 counter — returns to battlefield without abilities") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Retched Wretch")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wretch = game.findPermanent("Retched Wretch")!!

                // Place a -1/-1 counter on the Wretch (4/2 -> 3/1).
                game.state = game.state.updateEntity(wretch) {
                    it.with(CountersComponent(mapOf(CounterType.MINUS_ONE_MINUS_ONE to 1)))
                }

                // Shock the Wretch — 2 damage to a 3/1 sends it to the graveyard.
                val castResult = game.castSpell(1, "Shock", wretch)
                withClue("Shock should be cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack() // resolve Shock; SBA puts the Wretch in the graveyard;
                                    // the dies trigger goes on the stack.
                game.resolveStack() // resolve the dies trigger; Wretch returns to battlefield
                                    // and loses all abilities.

                withClue("Retched Wretch should return to its owner's battlefield") {
                    game.isOnBattlefield("Retched Wretch") shouldBe true
                }

                val returnedWretch = game.findPermanent("Retched Wretch")!!
                val projected = StateProjector().project(game.state)
                withClue("The returned Wretch should have lost all abilities") {
                    projected.hasLostAllAbilities(returnedWretch) shouldBe true
                }
            }

            test("dies without a -1/-1 counter — stays in the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Retched Wretch")
                    .withCardInHand(1, "Sear") // 4 damage — kills 4/2 outright
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wretch = game.findPermanent("Retched Wretch")!!

                val castResult = game.castSpell(1, "Sear", wretch)
                withClue("Sear should be cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Retched Wretch should be in the graveyard (no -1/-1 counter to trigger return)") {
                    game.isOnBattlefield("Retched Wretch") shouldBe false
                    game.isInGraveyard(1, "Retched Wretch") shouldBe true
                }
            }
        }
    }
}
