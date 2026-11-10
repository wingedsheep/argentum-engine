package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Slayer's Plate (SOI #264).
 *
 * "Whenever equipped creature dies, if it was a Human, create a 1/1 white Spirit creature token
 *  with flying."
 *
 * The intervening-if reads the dead creature's last-known type line on the equipped-creature
 * binding: a Human card and a Human token (gone entirely by resolution) both make a Spirit, a
 * non-Human makes nothing.
 */
class SlayersPlateScenarioTest : ScenarioTestBase() {

    private fun killEquipped(host: String, isToken: Boolean = false): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, host, isToken = isToken)
            .withCardAttachedTo(1, "Slayer's Plate", host)
            .withCardInHand(1, "Murder")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val creature = game.findPermanent(host)!!
        withClue("equipped creature gets +4/+2") {
            game.state.projectedState.getPower(creature) shouldBe 6
        }
        game.castSpell(1, "Murder", creature).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        context("Slayer's Plate") {

            test("an equipped Human dying makes a 1/1 white flying Spirit") {
                val game = killEquipped("Elite Vanguard")

                game.isInGraveyard(1, "Elite Vanguard") shouldBe true
                val spirit = game.findAllPermanents("Spirit Token").single()
                game.state.projectedState.getPower(spirit) shouldBe 1
                game.state.projectedState.getToughness(spirit) shouldBe 1
                game.state.projectedState.hasKeyword(spirit, Keyword.FLYING) shouldBe true
            }

            test("a Human token reads as a Human by last-known information") {
                val game = killEquipped("Elite Vanguard", isToken = true)

                game.findPermanent("Elite Vanguard") shouldBe null
                game.findAllPermanents("Spirit Token").size shouldBe 1
            }

            test("an equipped non-Human dying makes nothing") {
                val game = killEquipped("Grizzly Bears")

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.findAllPermanents("Spirit Token").shouldBeEmpty()
            }
        }
    }
}
