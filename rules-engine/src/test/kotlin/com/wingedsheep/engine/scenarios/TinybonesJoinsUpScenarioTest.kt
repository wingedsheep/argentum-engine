package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Tinybones Joins Up (OTJ #108) — {B} Legendary Enchantment.
 *
 *   "When Tinybones Joins Up enters, any number of target players each discard a card.
 *    Whenever a legendary creature you control enters, any number of target players each
 *    mill a card and lose 1 life."
 *
 * Verifies the ETB makes each chosen player discard a card and the legendary-enters trigger
 * mills 1 + drains 1 from each chosen player.
 */
class TinybonesJoinsUpScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Legend",
                manaCost = ManaCost.parse("{1}"),
                subtypes = setOf(Subtype.HUMAN),
                supertypes = setOf(Supertype.LEGENDARY),
                power = 2,
                toughness = 2,
            )
        )

        context("Tinybones Joins Up") {

            test("ETB makes each targeted player discard a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Tinybones Joins Up")
                    .withCardInHand(1, "Swamp")
                    .withCardsInHand(2, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Tinybones Joins Up").error shouldBe null
                game.resolveStack()

                // Target both players.
                game.selectTargets(listOf(game.player1Id, game.player2Id))

                // Each affected player chooses a card to discard. The decision carries the player
                // it belongs to; submit a card from that player's hand. Loop until none remain.
                var guard = 0
                while (guard++ < 10) {
                    game.resolveStack()
                    val decision = game.getPendingDecision()
                    if (decision !is com.wingedsheep.engine.core.SelectCardsDecision) break
                    game.submitDecision(
                        com.wingedsheep.engine.core.CardsSelectedResponse(decision.id, listOf(decision.options.first()))
                    )
                }
                game.resolveStack()

                withClue("Player1 discarded their only card") {
                    game.graveyardSize(1) shouldBe 1
                }
                withClue("Player2 discarded one Mountain") {
                    game.findCardsInGraveyard(2, "Mountain").size shouldBe 1
                }
            }

            test("legendary creature entering mills 1 and drains 1 from each targeted player") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Test Legend")
                    .withCardOnBattlefield(1, "Tinybones Joins Up")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val p2LifeBefore = game.getLifeTotal(2)
                val p2LibBefore = game.librarySize(2)

                game.castSpell(1, "Test Legend").error shouldBe null
                game.resolveStack()

                // The legendary-enters trigger targets only Player2.
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(game.player2Id))
                }
                game.resolveStack()

                withClue("Player2 milled a card") {
                    game.librarySize(2) shouldBe p2LibBefore - 1
                }
                withClue("Player2 lost 1 life") {
                    game.getLifeTotal(2) shouldBe p2LifeBefore - 1
                }
            }
        }
    }
}
