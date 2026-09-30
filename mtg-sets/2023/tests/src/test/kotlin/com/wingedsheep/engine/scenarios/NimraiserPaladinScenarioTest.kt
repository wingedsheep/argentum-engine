package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Nimraiser Paladin (ONE #101) — Toxic 2; when it enters, return target creature card with mana
 * value 3 or less from your graveyard to your hand.
 */
class NimraiserPaladinScenarioTest : ScenarioTestBase() {

    private fun castPaladin(vararg graveyard: String): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Nimraiser Paladin")
            .withLandsOnBattlefield(1, "Swamp", 5)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        graveyard.forEach { builder.withCardInGraveyard(1, it) }
        val game = builder.build()
        game.castSpell(1, "Nimraiser Paladin").error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        context("Nimraiser Paladin") {
            test("enters and returns a creature card with mana value 3 or less") {
                val game = castPaladin("Grizzly Bears", "Hill Giant")
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Hill Giant") shouldBe true
            }

            test("a creature card with mana value 4 is not a legal target") {
                val game = castPaladin("Hill Giant")
                // No legal target: the trigger is removed without asking for a choice.
                game.hasPendingDecision() shouldBe false
                game.resolveStack()

                (game.findPermanent("Nimraiser Paladin") != null) shouldBe true
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                game.isInHand(1, "Hill Giant") shouldBe false
            }
        }
    }
}
