package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Thirst for Discovery (VOW #85) — {2}{U} Instant.
 *
 *   Draw three cards. Then discard two cards unless you discard a basic land card.
 *
 * Exercises Effects.DrawCards(3).then(Effects.DiscardUnlessMatching(2, GameObjectFilter.BasicLand)):
 * discarding a single basic land satisfies the discard instruction; otherwise two cards must be
 * discarded.
 */
class ThirstForDiscoveryScenarioTest : ScenarioTestBase() {

    init {
        context("Thirst for Discovery — draw three, conditional discard") {

            test("discarding one basic land satisfies the discard instruction") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Thirst for Discovery")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Forest")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Glory Seeker")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Thirst for Discovery").error shouldBe null
                game.resolveStack()

                withClue("Draws three cards, then pauses for the conditional discard") {
                    game.hasPendingDecision() shouldBe true
                }
                val decision = game.getPendingDecision() as SelectCardsDecision
                val forestId = game.findCardsInHand(1, "Forest").single()

                withClue("A single basic land card satisfies the discard requirement") {
                    decision.minSelections shouldBe 1
                    decision.conditionalMinimums.single().matchingOptions shouldContain forestId
                }

                game.selectCards(listOf(forestId)).error shouldBe null

                withClue("The basic land is discarded and nothing else") {
                    game.isInGraveyard(1, "Forest") shouldBe true
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
            }

            test("discarding two nonland cards satisfies the normal discard count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Thirst for Discovery")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Glory Seeker")
                    .withCardInLibrary(1, "Savannah Lions")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Thirst for Discovery").error shouldBe null
                game.resolveStack()

                val bearsId = game.findCardsInHand(1, "Grizzly Bears").single()
                val giantId = game.findCardsInHand(1, "Hill Giant").single()

                game.selectCards(listOf(bearsId, giantId)).error shouldBe null

                withClue("Both nonland cards discarded") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
            }
        }
    }
}
