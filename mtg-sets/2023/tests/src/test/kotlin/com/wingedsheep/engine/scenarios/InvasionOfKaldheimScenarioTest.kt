package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.mtg.sets.definitions.mom.cards.InvasionOfKaldheim

/** Invasion of Kaldheim // Pyre of the World Tree. */
class InvasionOfKaldheimScenarioTest : ScenarioTestBase() {
    init {
        test("front: exiles the hand, draws that many, and the exiled cards stay playable") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Kaldheim")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Kaldheim").error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.isInExile(1, "Hill Giant") shouldBe true
            game.handSize(1) shouldBe 2

            game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("back: discarding a land deals 2 damage and exiles the top card to play") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Invasion of Kaldheim")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Forest")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.checkStateBasedActions()
            repeat(2) {
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Invasion of Kaldheim")!!).error shouldBe null
                game.resolveStack()
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            val pyre = game.findPermanent("Pyre of the World Tree")!!
            val abilityId = InvasionOfKaldheim.backFace!!.activatedAbilities.single().id
            val before = game.getLifeTotal(2)
            val r = game.execute(
                ActivateAbility(
                    game.player1Id, pyre, abilityId,
                    targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(game.player2Id)),
                    costPayment = AdditionalCostPayment(discardedCards = game.findCardsInHand(1, "Forest")),
                )
            )
            r.error shouldBe null
            (game.getPendingDecision() as? SelectCardsDecision)?.let { game.selectCards(it.options.take(1)) }
            game.resolveStack()

            game.getLifeTotal(2) shouldBe before - 2
            game.isInGraveyard(1, "Forest") shouldBe true
            game.isInExile(1, "Mountain") shouldBe true
        }
    }
}
