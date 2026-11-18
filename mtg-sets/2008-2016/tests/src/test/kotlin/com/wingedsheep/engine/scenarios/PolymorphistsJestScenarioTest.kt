package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Polymorphist's Jest.
 *
 * "Until end of turn, each creature target player controls loses all abilities and becomes a blue
 * Frog with base power and toughness 1/1."
 *
 * The first card to drive the colour- and creature-type-setting effects through a `ForEachInGroup`
 * iteration, so this proves each of the target player's creatures gets the whole Frog package —
 * and, per the card's ruling, that a creature arriving after resolution does not.
 */
class PolymorphistsJestScenarioTest : ScenarioTestBase() {

    private fun game() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Polymorphist's Jest")
        .withLandsOnBattlefield(1, "Island", 3)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Serra Angel")
        .withCardOnBattlefield(2, "Ornithopter")
        .withCardInHand(2, "Grizzly Bears")
        .withLandsOnBattlefield(2, "Forest", 2)
        .withActivePlayer(2)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Polymorphist's Jest") {
            test("each creature the target player controls becomes an abilityless blue 1/1 Frog") {
                val game = game()
                val angel = game.findPermanent("Serra Angel")!!
                val thopter = game.findPermanent("Ornithopter")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpellTargetingPlayer(1, "Polymorphist's Jest", 2).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                for (frog in listOf(angel, thopter)) {
                    withClue("base 1/1") {
                        projected.getPower(frog) shouldBe 1
                        projected.getToughness(frog) shouldBe 1
                    }
                    withClue("loses flying and vigilance") {
                        projected.hasKeyword(frog, Keyword.FLYING) shouldBe false
                        projected.hasKeyword(frog, Keyword.VIGILANCE) shouldBe false
                    }
                    withClue("blue is its only colour") { projected.getColors(frog) shouldBe setOf("BLUE") }
                    withClue("Frog is its only creature type") { projected.getSubtypes(frog) shouldBe setOf("Frog") }
                }
                withClue("Ornithopter keeps its artifact card type") {
                    projected.hasType(thopter, "ARTIFACT") shouldBe true
                }
                withClue("the caster's own creature is not affected") {
                    projected.getPower(giant) shouldBe 3
                    projected.getToughness(giant) shouldBe 3
                    projected.hasSubtype(giant, "Frog") shouldBe false
                }
            }

            test("a creature the target player gets after resolution is not affected") {
                val game = game()

                game.castSpellTargetingPlayer(1, "Polymorphist's Jest", 2).error shouldBe null
                game.resolveStack()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 2
                projected.getToughness(bears) shouldBe 2
                projected.getColors(bears) shouldBe setOf("GREEN")
                projected.hasSubtype(bears, "Frog") shouldBe false
            }
        }
    }
}
