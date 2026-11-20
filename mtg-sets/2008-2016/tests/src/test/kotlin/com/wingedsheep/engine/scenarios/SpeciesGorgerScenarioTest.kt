package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Species Gorger (DGM #105) — {3}{G}{U} 6/6 Creature — Frog Beast.
 *
 *   At the beginning of your upkeep, return a creature you control to its owner's hand.
 */
class SpeciesGorgerScenarioTest : ScenarioTestBase() {

    private fun upkeep(withBears: Boolean): TestGame {
        val builder = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, "Species Gorger")
            .withCardOnBattlefield(2, "Hill Giant")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UNTAP)
        if (withBears) builder.withCardOnBattlefield(1, "Grizzly Bears")
        val game = builder.build()
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    init {
        test("you choose which of your creatures goes back to hand") {
            val game = upkeep(withBears = true)

            withClue("a choice between your creatures is pending") { game.hasPendingDecision() shouldBe true }
            game.selectCards(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Species Gorger") shouldBe true
            withClue("the opponent's creature is never a choice") { game.isOnBattlefield("Hill Giant") shouldBe true }
        }

        test("as your only creature, it returns itself") {
            val game = upkeep(withBears = false)

            if (game.hasPendingDecision()) {
                game.selectCards(listOf(game.findPermanent("Species Gorger")!!)).error shouldBe null
                game.resolveStack()
            }

            game.isInHand(1, "Species Gorger") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
