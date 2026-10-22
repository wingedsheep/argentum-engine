package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Shipwreck Sifters (MID #74) — {1}{U} Creature — Spirit 1/2.
 *
 *   When this creature enters, draw a card, then discard a card.
 *   Whenever you discard a Spirit card or a card with disturb, put a +1/+1 counter on this creature.
 *
 * Exercises the discard trigger's card filter against the discarded card: a Spirit card and a
 * card with disturb (Baithook Angler, a Human Peasant) each grow it; any other card doesn't.
 */
class ShipwreckSiftersScenarioTest : ScenarioTestBase() {

    private fun castAndDiscard(discardName: String): Int {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Shipwreck Sifters")
            .withCardInHand(1, discardName)
            .withCardInLibrary(1, "Island")
            .withLandsOnBattlefield(1, "Island", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.castSpell(1, "Shipwreck Sifters").error shouldBe null
        game.resolveStack()

        game.getPendingDecision() as? SelectCardsDecision
            ?: error("expected the loot's discard selection; got ${game.getPendingDecision()}")
        game.selectCards(game.findCardsInHand(1, discardName)).error shouldBe null
        game.resolveStack()

        withClue("$discardName was discarded") {
            game.isInGraveyard(1, discardName) shouldBe true
        }
        val sifters = game.findPermanent("Shipwreck Sifters")!!
        return game.state.getEntity(sifters)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Shipwreck Sifters") {
            test("discarding a card with disturb puts a +1/+1 counter on it") {
                castAndDiscard("Baithook Angler") shouldBe 1
            }

            test("discarding a Spirit card puts a +1/+1 counter on it") {
                castAndDiscard("Phantom Carriage") shouldBe 1
            }

            test("discarding any other card does not") {
                castAndDiscard("Grizzly Bears") shouldBe 0
            }
        }
    }
}
