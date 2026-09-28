package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.mtg.sets.definitions.mom.cards.InvasionOfMercadia

/** Invasion of Mercadia // Kyren Flamewright. */
class InvasionOfMercadiaScenarioTest : ScenarioTestBase() {
    init {
        test("front: discarding a card draws two") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Mercadia")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Mercadia").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            (game.getPendingDecision() as? SelectCardsDecision)?.let {
                game.selectCards(it.options.take(1)).error shouldBe null
            }
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.handSize(1) shouldBe 2
        }

        test("front: declining draws nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Mercadia")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Mercadia").error shouldBe null
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe 1
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
        }

        test("back: Kyren Flamewright makes two Elementals, then pumps and hastes every creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Invasion of Mercadia")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.checkStateBasedActions()
            repeat(2) {
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Invasion of Mercadia")!!).error shouldBe null
                game.resolveStack()
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            val flame = game.findPermanent("Kyren Flamewright")!!
            game.state = game.state.updateEntity(flame) {
                it.without<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>()
            }
            val abilityId = InvasionOfMercadia.backFace!!.activatedAbilities.single().id
            game.execute(ActivateAbility(
                game.player1Id, flame, abilityId,
                costPayment = AdditionalCostPayment(discardedCards = game.findCardsInHand(1, "Grizzly Bears")),
            )).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            (game.getPendingDecision() as? SelectCardsDecision)?.let {
                game.selectCards(it.options.take(1)).error shouldBe null
            }
            game.resolveStack()

            val elementals = game.state.getBattlefield().filter {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Elemental Token"
            }
            elementals.size shouldBe 2
            elementals.forEach {
                game.state.projectedState.getPower(it) shouldBe 2
                game.state.projectedState.hasKeyword(it, com.wingedsheep.sdk.core.Keyword.HASTE) shouldBe true
            }
            game.state.projectedState.getPower(flame) shouldBe 4
        }
    }
}
