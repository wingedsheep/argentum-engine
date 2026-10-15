package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vampire Scrivener (SNC #98) — {4}{B} 2/2 Creature — Vampire Warlock, Flying.
 *
 * "Whenever you gain life during your turn, put a +1/+1 counter on this creature.
 *  Whenever you lose life during your turn, put a +1/+1 counter on this creature."
 */
class VampireScrivenerScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Vampire Scrivener") {
            test("gaining and losing life during your turn each add a +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vampire Scrivener")
                    .withCardInHand(1, "Sacred Nectar")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val scrivener = game.findPermanent("Vampire Scrivener")!!

                game.castSpell(1, "Sacred Nectar").error shouldBe null
                game.resolveStack()
                withClue("gaining life on your turn adds a counter") { game.plusCounters(scrivener) shouldBe 1 }

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                withClue("losing life on your turn adds another counter") { game.plusCounters(scrivener) shouldBe 2 }
            }

            test("losing life during an opponent's turn does nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vampire Scrivener")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val scrivener = game.findPermanent("Vampire Scrivener")!!

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()

                withClue("Player1 lost life, but not during their own turn") {
                    game.getLifeTotal(1) shouldBe 17
                    game.plusCounters(scrivener) shouldBe 0
                }
            }
        }
    }
}
