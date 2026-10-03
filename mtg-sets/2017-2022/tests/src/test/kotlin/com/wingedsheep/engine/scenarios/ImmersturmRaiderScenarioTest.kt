package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Immersturm Raider (KHM #141) — "When this creature enters, you may discard a card. If you do,
 * draw a card."
 */
class ImmersturmRaiderScenarioTest : ScenarioTestBase() {
    init {
        fun setup() = scenario()
            .withPlayers("P1", "P2")
            .withCardInHand(1, "Immersturm Raider")
            .withCardInHand(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardInLibrary(1, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("accepting discards a card and then draws one") {
            val game = setup()
            game.castSpell(1, "Immersturm Raider").error shouldBe null
            game.resolveStack()

            val bears = game.findCardsInHand(1, "Grizzly Bears").single()
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            if (game.hasPendingDecision()) game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("the discarded card is in the graveyard") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
            withClue("the draw replaced it") {
                game.isInHand(1, "Island") shouldBe true
                game.handSize(1) shouldBe 1
            }
        }

        test("declining neither discards nor draws") {
            val game = setup()
            game.castSpell(1, "Immersturm Raider").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Island") shouldBe false
            game.librarySize(1) shouldBe 1
        }
    }
}
