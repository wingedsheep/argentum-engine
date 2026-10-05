package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Built to Last — {W} Instant (Kaladesh #7, reprinted in Jumpstart 2022)
 *
 * "Target creature gets +2/+2 until end of turn. If it's an artifact creature, it gains
 *  indestructible until end of turn."
 *
 * Ruling (2016-09-20): an artifact creature gains indestructible *in addition to* getting
 * +2/+2, not instead of it. A nonartifact creature gets only the pump — proved by Murder still
 * destroying it — and an artifact creature gets both, proved by Murder failing to.
 */
class BuiltToLastScenarioTest : ScenarioTestBase() {

    private fun board(target: String, targetController: Int = 1): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Built to Last")
            .withCardInHand(1, "Murder")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardOnBattlefield(targetController, target)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(3) { builder = builder.withCardInLibrary(1, "Plains") }
        repeat(3) { builder = builder.withCardInLibrary(2, "Plains") }
        return builder.build()
    }

    private fun TestGame.castAndResolve(spell: String, target: String) {
        val id = findPermanent(target)!!
        val cast = castSpell(1, spell, id)
        withClue("Casting $spell should succeed: ${cast.error}") { cast.error shouldBe null }
        resolveStack()
    }

    init {
        context("Built to Last") {

            test("an artifact creature gets +2/+2 and indestructible, and survives a destroy effect") {
                val game = board("Titanium Golem") // 3/3 artifact creature
                game.castAndResolve("Built to Last", "Titanium Golem")

                val golem = game.findPermanent("Titanium Golem")!!
                val projected = game.state.projectedState
                withClue("The pump applies in addition to indestructible (ruling)") {
                    projected.getPower(golem) shouldBe 5
                    projected.getToughness(golem) shouldBe 5
                }
                withClue("An artifact creature gains indestructible") {
                    projected.hasKeyword(golem, Keyword.INDESTRUCTIBLE) shouldBe true
                }

                game.castAndResolve("Murder", "Titanium Golem")
                withClue("Indestructible Titanium Golem survives Murder") {
                    game.isOnBattlefield("Titanium Golem") shouldBe true
                }
            }

            test("a nonartifact creature gets only +2/+2 and can still be destroyed") {
                val game = board("Grizzly Bears") // 2/2, nonartifact
                game.castAndResolve("Built to Last", "Grizzly Bears")

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 4
                projected.getToughness(bears) shouldBe 4
                withClue("A nonartifact creature does not gain indestructible") {
                    projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe false
                }

                game.castAndResolve("Murder", "Grizzly Bears")
                withClue("Grizzly Bears is destroyed by Murder") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }

            test("can target an opponent's artifact creature, which also gains indestructible") {
                val game = board("Titanium Golem", targetController = 2)
                game.castAndResolve("Built to Last", "Titanium Golem")

                val golem = game.findPermanent("Titanium Golem")!!
                val projected = game.state.projectedState
                projected.getPower(golem) shouldBe 5
                projected.getToughness(golem) shouldBe 5
                projected.hasKeyword(golem, Keyword.INDESTRUCTIBLE) shouldBe true
            }

            test("both the pump and indestructible end at end of turn") {
                val game = board("Titanium Golem")
                game.castAndResolve("Built to Last", "Titanium Golem")
                game.state.projectedState.hasKeyword(game.findPermanent("Titanium Golem")!!, Keyword.INDESTRUCTIBLE) shouldBe true

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                val golem = game.findPermanent("Titanium Golem")!!
                val projected = game.state.projectedState
                projected.getPower(golem) shouldBe 3
                projected.getToughness(golem) shouldBe 3
                projected.hasKeyword(golem, Keyword.INDESTRUCTIBLE) shouldBe false
            }
        }
    }
}
