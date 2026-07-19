package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Feed the Cauldron — {2}{B} Instant — "Destroy target creature with mana value 3 or less.
 * If it's your turn, create a Food token."
 *
 * Covers the mana-value target restriction and the turn-gated Food half (which is checked on
 * resolution, so casting on the opponent's turn destroys but makes no Food).
 */
class FeedTheCauldronScenarioTest : ScenarioTestBase() {

    private fun game(opponentCreature: String, activePlayer: Int) = scenario()
        .withPlayers()
        .withCardInHand(1, "Feed the Cauldron")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardOnBattlefield(2, opponentCreature)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Forest")
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .withActivePlayer(activePlayer)
        .withPriorityPlayer(1)
        .build()

    init {
        test("on your turn: destroys a mana value 3 creature and creates a Food") {
            val g = game("Centaur Courser", activePlayer = 1)
            val target = g.findPermanent("Centaur Courser")!!

            g.castSpell(1, "Feed the Cauldron", target).error shouldBe null
            g.resolveStack()

            g.isOnBattlefield("Centaur Courser") shouldBe false
            g.isInGraveyard(2, "Centaur Courser") shouldBe true
            g.findPermanents("Food").size shouldBe 1
        }

        test("on the opponent's turn: destroys the creature but creates no Food") {
            val g = game("Centaur Courser", activePlayer = 2)
            val target = g.findPermanent("Centaur Courser")!!

            g.castSpell(1, "Feed the Cauldron", target).error shouldBe null
            g.resolveStack()

            g.isOnBattlefield("Centaur Courser") shouldBe false
            g.findPermanents("Food").size shouldBe 0
        }

        test("cannot target a creature with mana value 4 or greater") {
            val g = game("Force of Nature", activePlayer = 1) // 5/5 for {3}{G}{G}
            val target = g.findPermanent("Force of Nature")!!

            g.castSpell(1, "Feed the Cauldron", target).error shouldNotBe null

            g.isOnBattlefield("Force of Nature") shouldBe true
            g.findPermanents("Food").size shouldBe 0
        }
    }
}
