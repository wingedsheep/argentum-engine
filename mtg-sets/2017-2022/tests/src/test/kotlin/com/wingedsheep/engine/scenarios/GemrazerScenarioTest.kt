package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Gemrazer (IKO #155) — {3}{G} Creature — Beast 4/4.
 *
 *   Mutate {1}{G}{G}
 *   Reach, trample
 *   Whenever this creature mutates, destroy target artifact or enchantment an opponent controls.
 *
 * Mutating over Grizzly Bears makes the Bears a 4/4 Gemrazer; under, it stays a 2/2 Bears with
 * reach and trample. Either way the mutate trigger destroys the opponent's Ornithopter, and a
 * normal cast — which doesn't mutate — leaves it alone.
 */
class GemrazerScenarioTest : ScenarioTestBase() {

    private fun TestGame.yourBears() = findPermanents("Grizzly Bears").single {
        state.projectedState.getController(it) == player1Id
    }

    private fun TestGame.mutateOntoBears() {
        val bears = yourBears()
        val offer = getLegalActions(1).single { (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.MUTATE }
        withClue("mutate may target the Bears, not the opponent's creature") {
            offer.validTargets?.toSet() shouldBe setOf(bears)
        }
        execute((offer.action as CastSpell).copy(targets = listOf(ChosenTarget.Permanent(bears)))).error shouldBe null
        resolveStack()
    }

    private fun scenarioWithBears() = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Gemrazer")
        .withLandsOnBattlefield(1, "Forest", 4)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Ornithopter")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Gemrazer") {

            test("mutating over a creature makes it Gemrazer and destroys an opponent's artifact") {
                val game = scenarioWithBears()
                val bears = game.yourBears()
                game.mutateOntoBears()
                withClue("the spell's controller chooses over or under") {
                    (game.state.pendingDecision is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true)
                game.selectTargets(listOf(game.findPermanent("Ornithopter")!!))
                game.resolveStack()

                withClue("the opponent's Ornithopter is destroyed") {
                    game.isInGraveyard(2, "Ornithopter") shouldBe true
                }
                withClue("the Bears' permanent is now a 4/4 Gemrazer with reach and trample") {
                    val projected = game.state.projectedState
                    game.findPermanent("Gemrazer") shouldBe bears
                    projected.getPower(bears) shouldBe 4
                    projected.getToughness(bears) shouldBe 4
                    projected.hasKeyword(bears, Keyword.REACH) shouldBe true
                    projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("mutating under keeps the Bears' size and still triggers") {
                val game = scenarioWithBears()
                val bears = game.yourBears()
                game.mutateOntoBears()
                game.answerYesNo(false)
                game.selectTargets(listOf(game.findPermanent("Ornithopter")!!))
                game.resolveStack()

                game.isInGraveyard(2, "Ornithopter") shouldBe true
                withClue("a 2/2 Grizzly Bears with Gemrazer's reach and trample") {
                    game.findPermanent("Gemrazer") shouldBe null
                    val projected = game.state.projectedState
                    projected.getPower(bears) shouldBe 2
                    projected.hasKeyword(bears, Keyword.REACH) shouldBe true
                    projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("cast normally it enters as a creature and doesn't mutate") {
                val game = scenarioWithBears()
                game.castSpell(1, "Gemrazer").error shouldBe null
                game.resolveStack()

                withClue("Gemrazer is its own permanent beside the Bears") {
                    game.isOnBattlefield("Gemrazer") shouldBe true
                    game.findPermanents("Grizzly Bears").size shouldBe 2
                }
                withClue("no mutate, so the Ornithopter survives") {
                    game.isOnBattlefield("Ornithopter") shouldBe true
                }
            }
        }
    }
}
