package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dark Prophecy (M14 #93) — "Whenever a creature you control dies, you draw a card and you lose 1 life."
 */
class DarkProphecyScenarioTest : ScenarioTestBase() {

    init {
        context("Dark Prophecy") {
            test("your creature dying draws a card and costs 1 life; an opponent's creature dying does nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dark Prophecy")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardsInHand(1, "Lightning Bolt", 2)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Lightning Bolt", game.findPermanent("Hill Giant")!!).error shouldBe null
                game.resolveStack()
                withClue("an opponent's creature dying does not trigger") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    game.getLifeTotal(1) shouldBe 20
                    game.librarySize(1) shouldBe 2
                }

                val handBefore = game.handSize(1)
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()
                withClue("your creature dying draws one card and loses 1 life") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.handSize(1) shouldBe handBefore
                    game.librarySize(1) shouldBe 1
                    game.getLifeTotal(1) shouldBe 19
                }
            }
        }
    }
}
