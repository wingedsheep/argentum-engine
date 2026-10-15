package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Maestros Initiate (SNC #86) — {2}{B} Creature — Human Citizen, 3/1.
 *
 *   {4}{U/R}, Exile this card from your graveyard: Draw two cards, then discard a card.
 *
 * The hybrid {U/R} is payable with red mana from a mono-red mana base.
 */
class MaestrosInitiateScenarioTest : ScenarioTestBase() {

    init {
        test("{4}{U/R}, exiling it from the graveyard, draws two then discards one") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Maestros Initiate")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Savannah Lions")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val initiate = game.findCardsInGraveyard(1, "Maestros Initiate").single()
            val abilityId = cardRegistry.getCard("Maestros Initiate")!!.activatedAbilities.first().id

            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = initiate, abilityId = abilityId))
                .error shouldBe null
            withClue("exiling the card is part of the cost") {
                game.isInExile(1, "Maestros Initiate") shouldBe true
            }
            game.resolveStack()

            val discard = game.getPendingDecision() as? SelectCardsDecision
                ?: error("expected a discard selection; got ${game.getPendingDecision()}")
            withClue("the discard is chosen from a hand holding both drawn cards") {
                discard.options.size shouldBe 3
            }
            game.selectCards(game.findCardsInHand(1, "Grizzly Bears")).error shouldBe null
            game.resolveStack()

            withClue("drew two, discarded Grizzly Bears") {
                game.isInHand(1, "Hill Giant") shouldBe true
                game.isInHand(1, "Savannah Lions") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe 2
            }
        }
    }
}
