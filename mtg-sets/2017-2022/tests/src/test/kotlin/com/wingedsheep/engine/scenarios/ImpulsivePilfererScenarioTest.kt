package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Impulsive Pilferer (CMR #185) — "When this creature dies, create a Treasure token. Encore {3}{R}"
 *
 * The encore token is a copy of the card, so when it is sacrificed at the end step its own dies
 * trigger makes a Treasure.
 */
class ImpulsivePilfererScenarioTest : ScenarioTestBase() {

    init {
        context("Impulsive Pilferer") {

            test("encore: a hasty copy must attack the opponent, then dies at the end step for a Treasure") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Impulsive Pilferer")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInGraveyard(1, "Impulsive Pilferer").single()
                val encore = game.getLegalActions(1).mapNotNull { it.action as? ActivateAbility }
                    .single { it.sourceId == cardId }
                game.execute(encore).error shouldBe null
                game.resolveStack()

                withClue("the card left the graveyard as part of the cost") {
                    game.findCardsInGraveyard(1, "Impulsive Pilferer").shouldBeEmpty()
                }
                game.findPermanents("Impulsive Pilferer") shouldHaveSize 1

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("the token must attack the opponent") {
                    game.declareAttackers(emptyMap()).error shouldNotBe null
                }
                withClue("it has haste, so it can") {
                    game.declareAttackers(mapOf("Impulsive Pilferer" to 2)).error shouldBe null
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.findPermanents("Impulsive Pilferer").shouldBeEmpty()
                withClue("the sacrificed copy's dies trigger made a Treasure") {
                    game.findPermanents("Treasure") shouldHaveSize 1
                }
            }

            test("dies: create a Treasure") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Impulsive Pilferer")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Lightning Bolt", game.findPermanent("Impulsive Pilferer")!!).error shouldBe null
                game.resolveStack()
                game.findPermanents("Treasure") shouldHaveSize 1
            }
        }
    }
}
