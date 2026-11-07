package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Go Forth (J25 #20) — "Choose one — • Search your library for a basic land card, reveal it, put it
 * into your hand, then shuffle. • Target creature gets +2/+2 until end of turn."
 */
class GoForthScenarioTest : ScenarioTestBase() {

    init {
        test("the search mode finds only basic lands and puts the choice into your hand") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Go Forth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val forest = game.findCardsInLibrary(1, "Forest").single()

            game.castSpellWithMode(1, "Go Forth", 0).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            decision.options shouldContainExactly listOf(forest)
            game.selectCards(listOf(forest)).error shouldBe null

            game.isInHand(1, "Forest") shouldBe true
            game.isInGraveyard(1, "Go Forth") shouldBe true
        }

        test("the pump mode gives target creature +2/+2") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Go Forth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpellWithMode(1, "Go Forth", 1, bears).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 4
        }
    }
}
