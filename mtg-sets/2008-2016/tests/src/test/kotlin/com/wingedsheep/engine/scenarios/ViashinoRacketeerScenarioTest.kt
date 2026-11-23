package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Viashino Racketeer (RTR) — When this creature enters, you may discard a card. If you do, draw a
 * card.
 */
class ViashinoRacketeerScenarioTest : ScenarioTestBase() {
    init {
        fun setup() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Viashino Racketeer")
            .withCardInHand(1, "Hill Giant")
            .withCardInHand(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("accepting discards the chosen card and draws one") {
            val game = setup()
            val cast = game.castSpell(1, "Viashino Racketeer")
            withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val giant = game.findCardsInHand(1, "Hill Giant").single()
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            withClue("Hill Giant discarded, Forest drawn, Bears kept") {
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                game.isInHand(1, "Forest") shouldBe true
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe 2
                game.librarySize(1) shouldBe 0
            }
        }

        test("declining discards nothing and draws nothing") {
            val game = setup()
            game.castSpell(1, "Viashino Racketeer").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            withClue("hand and library untouched") {
                game.handSize(1) shouldBe 2
                game.librarySize(1) shouldBe 1
                game.graveyardSize(1) shouldBe 0
            }
        }
    }
}
