package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Averna, the Chaos Bloom (Commander Legends #269) — {G}{U}{R} 4/2.
 * "As you cascade, you may put a land card from among the exiled cards onto the battlefield tapped."
 *
 * The card's wiring: Averna's static rides a real cascade (Quandrix, the Proof) and the land it
 * picks enters tapped before the cast offer. The rules matrix lives in CascadeMayPutOntoBattlefieldTest.
 */
class AvernaTheChaosBloomScenarioTest : ScenarioTestBase() {
    init {
        test("a land the cascade exiled enters tapped, then the hit is offered") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Quandrix, the Proof")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardOnBattlefield(1, "Averna, the Chaos Bloom")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Grizzly Bears")
                .build()

            game.castSpell(1, "Quandrix, the Proof").error shouldBe null
            game.resolveStack()

            val choice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val mountain = choice.options.single()
            game.state.getEntity(mountain)?.get<CardComponent>()?.name shouldBe "Mountain"
            game.selectCards(listOf(mountain))

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.state.getEntity(mountain)?.has<TappedComponent>() shouldBe true
            (mountain in game.state.getBattlefield()) shouldBe true
        }
    }
}
