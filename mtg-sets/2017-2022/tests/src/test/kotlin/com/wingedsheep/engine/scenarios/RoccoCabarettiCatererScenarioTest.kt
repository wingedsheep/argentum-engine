package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rocco, Cabaretti Caterer (SNC #218) — "When Rocco enters, if you cast it, you may search your
 * library for a creature card with mana value X or less, put it onto the battlefield, then shuffle."
 *
 * The part worth pinning is the cost-linked cap: the ETB trigger resolves after Rocco has left the
 * stack, so the search has to read the X paid for it off the permanent's cast record.
 */
class RoccoCabarettiCatererScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Rocco, Cabaretti Caterer")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withLandsOnBattlefield(1, "Plains", 5)
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Hill Giant")
        .withCardInLibrary(1, "Craw Wurm")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Rocco, Cabaretti Caterer") {

            test("X = 2 offers only creatures with mana value 2 or less and puts the pick onto the battlefield") {
                val game = board()
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()

                game.castXSpell(1, "Rocco, Cabaretti Caterer", xValue = 2).error shouldBe null
                game.resolveStack()
                game.answerYesNo(true)

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Hill Giant (MV 4) and Craw Wurm (MV 6) exceed X = 2") {
                    decision.options shouldContainExactlyInAnyOrder listOf(bears)
                }

                game.selectCards(listOf(bears))
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe false
            }

            test("X = 4 widens the search to Hill Giant") {
                val game = board()
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                val giant = game.findCardsInLibrary(1, "Hill Giant").single()

                game.castXSpell(1, "Rocco, Cabaretti Caterer", xValue = 4).error shouldBe null
                game.resolveStack()
                game.answerYesNo(true)

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.options shouldContainExactlyInAnyOrder listOf(bears, giant)

                game.selectCards(listOf(giant))
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }
    }
}
