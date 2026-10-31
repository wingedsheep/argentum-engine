package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Brightwood Tracker (M20 #166) — {3}{G} 2/4 Creature — Elf Scout.
 *
 *   {5}{G}, {T}: Look at the top four cards of your library. You may reveal a creature card from
 *   among them and put it into your hand. Put the rest on the bottom of your library in a random order.
 */
class BrightwoodTrackerScenarioTest : ScenarioTestBase() {

    init {
        context("Brightwood Tracker") {

            fun build() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Brightwood Tracker")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.activateTracker() {
                val tracker = findPermanent("Brightwood Tracker")!!
                val abilityId = cardRegistry.getCard("Brightwood Tracker")!!.script.activatedAbilities[0].id
                execute(ActivateAbility(playerId = player1Id, sourceId = tracker, abilityId = abilityId))
                    .error shouldBe null
                resolveStack()
            }

            test("takes one creature card from the top four and bottoms the rest") {
                val game = build()
                val giant = game.findCardsInLibrary(1, "Hill Giant").single()

                game.activateTracker()
                game.hasPendingDecision() shouldBe true
                game.selectCards(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Hill Giant") shouldBe true
                game.isInHand(1, "Grizzly Bears") shouldBe false
                game.handSize(1) shouldBe 1
                game.librarySize(1) shouldBe 3
            }

            test("may take nothing; all four go back to the library") {
                val game = build()

                game.activateTracker()
                game.hasPendingDecision() shouldBe true
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 0
                game.librarySize(1) shouldBe 4
            }
        }
    }
}
