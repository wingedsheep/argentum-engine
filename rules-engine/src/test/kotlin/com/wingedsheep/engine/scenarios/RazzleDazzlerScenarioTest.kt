package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Razzle-Dazzler (OTJ #63) — {1}{U} 1/2 Creature — Human Wizard.
 *
 *   "Whenever you cast your second spell each turn, put a +1/+1 counter on this creature.
 *    It can't be blocked this turn."
 *
 * Verifies the trigger fires on the controller's *second* spell each turn (not the first),
 * adding exactly one +1/+1 counter and granting CANT_BE_BLOCKED until end of turn.
 */
class RazzleDazzlerScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame): Int {
        val id = game.findPermanent("Razzle-Dazzler") ?: return 0
        return game.state.getEntity(id)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Razzle-Dazzler") {

            test("second spell each turn adds a +1/+1 counter and grants can't-be-blocked; first does not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Razzle-Dazzler")
                    .withCardInHand(1, "Grizzly Bears") // first spell, {1}{G}
                    .withCardInHand(1, "Goblin Guide")  // second spell, {R}
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dazzler = game.findPermanent("Razzle-Dazzler")!!

                // First spell: trigger must NOT fire.
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                withClue("First spell does not trigger Razzle-Dazzler") {
                    plusOneCounters(game) shouldBe 0
                    game.state.projectedState
                        .hasKeyword(dazzler, "CANT_BE_BLOCKED") shouldBe false
                }

                // Second spell: trigger fires once.
                game.castSpell(1, "Goblin Guide").error shouldBe null
                game.resolveStack()
                withClue("Second spell adds exactly one +1/+1 counter") {
                    plusOneCounters(game) shouldBe 1
                }
                withClue("Second spell grants CANT_BE_BLOCKED this turn") {
                    game.state.projectedState
                        .hasKeyword(dazzler, "CANT_BE_BLOCKED") shouldBe true
                }
            }
        }
    }
}
