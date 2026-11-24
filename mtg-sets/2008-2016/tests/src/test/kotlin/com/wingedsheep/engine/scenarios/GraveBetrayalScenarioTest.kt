package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Grave Betrayal (RTR #67) — {5}{B}{B} Enchantment.
 *
 * "Whenever a creature you don't control dies, return it to the battlefield under your control with
 *  an additional +1/+1 counter on it at the beginning of the next end step. That creature is a black
 *  Zombie in addition to its other colors and types."
 *
 * Pins the delayed return: the opponent's creature waits in the graveyard until the end step (the
 * following turn's when it died during an end step), then comes back under Grave Betrayal's
 * controller with one +1/+1 counter as a black Zombie on top of its own colour and types. Your own
 * creatures, tokens, and a card that left the graveyard first never return (2012-10-01 rulings).
 */
class GraveBetrayalScenarioTest : ScenarioTestBase() {

    private fun killBears(
        phase: Phase = Phase.PRECOMBAT_MAIN,
        step: Step = Step.PRECOMBAT_MAIN,
        bearsController: Int = 2,
        bearsIsToken: Boolean = false
    ): TestGame {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, "Grave Betrayal")
            .withCardOnBattlefield(bearsController, "Grizzly Bears", isToken = bearsIsToken)
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .inPhase(phase, step)
            .build()

        val bears = game.findPermanent("Grizzly Bears")!!
        game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
        game.resolveStack()
        game.checkStateBasedActions()
        return game
    }

    private fun TestGame.toEndStepAndResolve() {
        passUntilPhase(Phase.ENDING, Step.END)
        resolveStack()
        checkStateBasedActions()
    }

    init {
        context("Grave Betrayal") {

            test("an opponent's creature returns at the end step under your control as a black Zombie with a +1/+1 counter") {
                val game = killBears()

                withClue("it waits in its owner's graveyard until the end step") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }

                game.toEndStepAndResolve()

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState
                withClue("it returned under Alice's control") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                    projected.getController(bears) shouldBe game.player1Id
                }
                withClue("it entered with one +1/+1 counter") {
                    game.state.getEntity(bears)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                    projected.getPower(bears) shouldBe 3
                    projected.getToughness(bears) shouldBe 3
                }
                withClue("it is a black Zombie in addition to its other colours and types") {
                    projected.getColors(bears) shouldBe setOf("GREEN", "BLACK")
                    projected.getSubtypes(bears) shouldContainAll setOf("Bear", "Zombie")
                }
            }

            test("a creature that dies during an end step returns at the next turn's end step") {
                val game = killBears(phase = Phase.ENDING, step = Step.END)
                val turnOfDeath = game.state.turnNumber

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("this turn's end step had already begun, so the card stayed in the graveyard") {
                    game.state.turnNumber shouldBe turnOfDeath + 1
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }

                game.toEndStepAndResolve()

                withClue("the following turn's end step returns it under Alice's control") {
                    game.state.turnNumber shouldBe turnOfDeath + 1
                    val bears = game.findPermanent("Grizzly Bears")!!
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                }
            }

            test("your own creature dying doesn't trigger it") {
                val game = killBears(bearsController = 1)

                game.toEndStepAndResolve()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }

            test("a card that leaves the graveyard before the end step doesn't return") {
                val game = killBears()
                val bears = game.state.getGraveyard(game.player2Id).single()
                game.state = zones.moveToZone(game.state, bears, Zone.EXILE).state
                game.state = zones.moveToZone(game.state, bears, Zone.GRAVEYARD).state

                game.toEndStepAndResolve()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }

            test("a token creature doesn't return") {
                val game = killBears(bearsIsToken = true)

                game.toEndStepAndResolve()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
        }
    }
}
