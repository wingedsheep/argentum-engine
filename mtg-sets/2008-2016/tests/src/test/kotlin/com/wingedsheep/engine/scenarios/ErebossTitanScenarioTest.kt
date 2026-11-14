package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Erebos's Titan (ORI #94) — "As long as your opponents control no creatures, this creature has
 * indestructible. Whenever a creature card leaves an opponent's graveyard, you may discard a card.
 * If you do, return this card from your graveyard to your hand."
 *
 * Worth proving: the indestructible grant reads only the opponents' side of the board, and the
 * recursion trigger fires from the graveyard on a card owned by an opponent leaving *their*
 * graveyard — never on one leaving yours — with the discard gating the return.
 */
class ErebossTitanScenarioTest : ScenarioTestBase() {

    init {
        context("Erebos's Titan") {

            test("indestructible while opponents control no creatures — your own creatures don't matter") {
                val alone = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Erebos's Titan")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val titan = alone.findPermanent("Erebos's Titan")!!
                alone.state.projectedState.hasKeyword(titan, Keyword.INDESTRUCTIBLE) shouldBe true

                val contested = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Erebos's Titan")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val contestedTitan = contested.findPermanent("Erebos's Titan")!!
                contested.state.projectedState.hasKeyword(contestedTitan, Keyword.INDESTRUCTIBLE) shouldBe false
            }

            test("a creature card leaving an opponent's graveyard lets you discard to return the Titan") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Erebos's Titan")
                    .withCardInHand(1, "Hill Giant")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInHand(2, "Raise Dead")
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(2, "Raise Dead", 2, "Grizzly Bears").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                if (game.getPendingDecision() is SelectCardsDecision) {
                    game.selectCards(game.findCardsInHand(1, "Hill Giant")).error shouldBe null
                }
                game.resolveStack()

                withClue("the discard was paid, so the Titan comes back to hand") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                    game.isInHand(1, "Erebos's Titan") shouldBe true
                    game.isInGraveyard(1, "Erebos's Titan") shouldBe false
                }
            }

            test("declining the discard leaves the Titan in the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Erebos's Titan")
                    .withCardInHand(1, "Hill Giant")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInHand(2, "Raise Dead")
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(2, "Raise Dead", 2, "Grizzly Bears").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Hill Giant") shouldBe true
                game.isInGraveyard(1, "Erebos's Titan") shouldBe true
            }

            test("a creature card leaving your own graveyard doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Erebos's Titan")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(1, "Hill Giant")
                    .withCardInHand(1, "Raise Dead")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Raise Dead", 1, "Grizzly Bears").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(1, "Erebos's Titan") shouldBe true
            }
        }
    }
}
