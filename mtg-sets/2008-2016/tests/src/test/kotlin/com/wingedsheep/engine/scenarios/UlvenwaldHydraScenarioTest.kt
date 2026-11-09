package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ulvenwald Hydra (SOI #235) — {4}{G}{G} * / * Hydra with reach.
 *
 *   Ulvenwald Hydra's power and toughness are each equal to the number of lands you control.
 *   When this creature enters, you may search your library for a land card, put it onto the
 *   battlefield tapped, then shuffle.
 */
class UlvenwaldHydraScenarioTest : ScenarioTestBase() {

    private fun build() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Ulvenwald Hydra")
        .withLandsOnBattlefield(1, "Forest", 6)
        .withLandsOnBattlefield(2, "Mountain", 3)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("fetching a land tapped grows it to the new land count") {
            val game = build()
            val island = game.findCardsInLibrary(1, "Island").single()

            game.castSpell(1, "Ulvenwald Hydra").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            val search = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            search.options shouldBe listOf(island)
            game.selectCards(listOf(island)).error shouldBe null
            game.resolveStack()

            game.state.getBattlefield().contains(island) shouldBe true
            game.state.getEntity(island)?.has<TappedComponent>() shouldBe true
            val hydra = game.findPermanent("Ulvenwald Hydra")!!
            game.state.projectedState.getPower(hydra) shouldBe 7
            game.state.projectedState.getToughness(hydra) shouldBe 7
            game.state.projectedState.hasKeyword(hydra, Keyword.REACH) shouldBe true
        }

        test("declining the search leaves it the size of your current lands, opponents' lands ignored") {
            val game = build()

            game.castSpell(1, "Ulvenwald Hydra").error shouldBe null
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.findCardsInLibrary(1, "Island").size shouldBe 1
            val hydra = game.findPermanent("Ulvenwald Hydra")!!
            game.state.projectedState.getPower(hydra) shouldBe 6
            game.state.projectedState.getToughness(hydra) shouldBe 6
        }
    }
}
