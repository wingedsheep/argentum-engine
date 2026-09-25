package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Distress (CHK #111) — "Target player reveals their hand. You choose a nonland card from it. That
 * player discards that card."
 *
 * Unlike Duress, a creature card is a legal pick and a land is not.
 */
class DistressScenarioTest : ScenarioTestBase() {

    init {
        context("Distress") {

            test("the caster picks a nonland card — creatures included — and the target discards it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Distress")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInHand(2, "Grizzly Bears").single()

                game.castSpellTargetingPlayer(1, "Distress", 2).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the caster chooses") { decision.playerId shouldBe game.player1Id }
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("the creature card was discarded") { game.isInGraveyard(2, "Grizzly Bears") shouldBe true }
                withClue("the land stays in hand") { game.isInHand(2, "Forest") shouldBe true }
            }

            test("a land can't be chosen") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Distress")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Distress", 2).error shouldBe null
                game.resolveStack()
                if (game.getPendingDecision() is SelectCardsDecision) {
                    game.selectCards(emptyList())
                    game.resolveStack()
                }

                withClue("the only card was a land, so nothing is discarded") {
                    game.isInHand(2, "Forest") shouldBe true
                }
            }
        }
    }
}
