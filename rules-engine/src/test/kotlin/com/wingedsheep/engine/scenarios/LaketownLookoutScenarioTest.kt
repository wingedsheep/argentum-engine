package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hob.cards.LaketownLookout
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Lake-town Lookout (HOB #18) — {W} Creature — Human Scout 1/1.
 *
 * "When this creature dies, recruit."
 *
 * The dies trigger has to fire from the graveyard-bound `detectLeavesBattlefieldTriggers` pass, and
 * recruit's discard decision must still surface after the Lookout itself has left the battlefield.
 */
class LaketownLookoutScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(LaketownLookout)

        context("Lake-town Lookout") {

            test("dying recruits: draw, discard a nonland, get a Soldier token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lake-town Lookout")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Lethal damage to the 1/1 → it dies to state-based actions → the dies trigger fires.
                val lookout = game.findPermanent("Lake-town Lookout")!!
                game.castSpell(1, "Lightning Bolt", lookout).error shouldBe null
                game.resolveStack()

                withClue("the Lookout should be in the graveyard") {
                    game.isInGraveyard(1, "Lake-town Lookout") shouldBe true
                }
                withClue("the dies trigger should pause for recruit's discard choice") {
                    game.hasPendingDecision() shouldBe true
                }

                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                game.selectCards(listOf(bears))
                game.resolveStack()

                withClue("recruit drew the Forest") {
                    game.isInHand(1, "Forest") shouldBe true
                }
                withClue("the nonland discard mints one Human Soldier token") {
                    game.findAllPermanents("Human Soldier Token").size shouldBe 1
                }
            }
        }
    }
}
