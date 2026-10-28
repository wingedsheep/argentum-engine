package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Selvala, Heart of the Wilds: "Whenever another creature enters, its controller may draw a card
 * if its power is greater than each other creature's power."
 *
 * The comparison spans every creature on the battlefield (both players, Selvala included) and is
 * strict — a tie means no one may draw.
 */
class SelvalaHeartOfTheWildsScenarioTest : ScenarioTestBase() {

    private fun setUp(opponentCreature: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Selvala, Heart of the Wilds")
        .withCardOnBattlefield(2, opponentCreature)
        .withCardInHand(1, "Hill Giant")
        .withLandsOnBattlefield(1, "Mountain", 4)
        .withCardInLibrary(1, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("a creature with strictly the greatest power lets its controller draw") {
            val game = setUp(opponentCreature = "Grizzly Bears")
            game.castSpell(1, "Hill Giant").error shouldBe null
            game.resolveStack()

            withClue("Hill Giant (3) beats Selvala (2) and Grizzly Bears (2)") {
                game.getPendingDecision()!!.playerId shouldBe game.player1Id
            }
            game.answerYesNo(true)

            game.handSize(1) shouldBe 1
            game.isInHand(1, "Grizzly Bears") shouldBe true
        }

        test("the draw is optional") {
            val game = setUp(opponentCreature = "Grizzly Bears")
            game.castSpell(1, "Hill Giant").error shouldBe null
            game.resolveStack()
            game.answerYesNo(false)

            game.handSize(1) shouldBe 0
        }

        test("a bigger creature anywhere on the battlefield denies the draw") {
            val game = setUp(opponentCreature = "Craw Wurm")
            game.castSpell(1, "Hill Giant").error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.handSize(1) shouldBe 0
        }

        test("a tie for greatest power denies the draw") {
            val game = setUp(opponentCreature = "Hill Giant")
            game.castSpell(1, "Hill Giant").error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.handSize(1) shouldBe 0
        }
    }
}
