package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.speed.SpeedService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Samut, the Driving Force — i.e. for the new
 * `CostReductionSource.YourSpeed`, plus the speed-scaled lord that has to agree with it.
 *
 * Oracle:
 * - "Other creatures you control get +X/+0, where X is your speed."
 * - "Noncreature spells you cast cost {X} less to cast, where X is your speed."
 *
 * The two clauses read the same number through different machinery — the lord through the layer
 * system, the reduction through `CostCalculator`, which never touches layers — so they are tested
 * separately and then against each other. The cases that would pass a naive implementation and still
 * be wrong: a *creature* spell getting the discount, the discount eating a colored pip, and the
 * discount following speed rather than being frozen at whatever it was when Samut entered.
 */
class SamutTheDrivingForceScenarioTest : ScenarioTestBase() {

    init {
        context("Samut's cost reduction — noncreature spells cost {X} less, X = your speed") {

            test("speed 1 (from Start your engines!) discounts a noncreature spell by 1") {
                val game = samutGame {
                    withCardInHand(1, "Divination")
                }

                withClue("Samut's own Start your engines! sets speed to 1 via the SBA") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                }

                withClue("Divination {2}{U} loses 1 generic") {
                    genericCostOf(game, "Divination", game.player1Id) shouldBe 1
                }
            }

            test("the discount tracks speed rather than being frozen at Samut's arrival") {
                val game = samutGame {
                    withCardInHand(1, "Harmonize")
                }

                withClue("Harmonize {2}{G}{G} at speed 1 loses 1 of its 2 generic") {
                    genericCostOf(game, "Harmonize", game.player1Id) shouldBe 1
                }

                game.state = SpeedService.set(game.state, game.player1Id, 3, "test").first
                withClue("speed 3 exceeds the 2 generic, which bottoms out at 0") {
                    genericCostOf(game, "Harmonize", game.player1Id) shouldBe 0
                }

                game.state = SpeedService.set(game.state, game.player1Id, Speed.STARTING, "test").first
                withClue("dropping back to speed 1 restores the discount to 1") {
                    genericCostOf(game, "Harmonize", game.player1Id) shouldBe 1
                }
            }

            test("the reduction never eats the colored pip") {
                val game = samutGame {
                    withCardInHand(1, "Harmonize")
                }
                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                val cost = CostCalculator(cardRegistry).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Harmonize"),
                    game.player1Id,
                )

                withClue("max speed (4) exceeds Harmonize's 2 generic but leaves {G}{G} intact") {
                    cost.genericAmount shouldBe 0
                    cost.colorCount[Color.GREEN] shouldBe 2
                }
            }

            test("creature spells are not discounted") {
                val game = samutGame {
                    withCardInHand(1, "Hill Giant")
                }
                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                withClue("Hill Giant {3}{R} is a creature spell, so its 3 generic stands") {
                    genericCostOf(game, "Hill Giant", game.player1Id) shouldBe 3
                }
            }

            test("the discount is the caster's speed, not the highest speed at the table") {
                val game = samutGame {
                    withCardInHand(2, "Divination")
                }
                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                withClue("the opponent controls no Samut, so their Divination is undiscounted") {
                    game.state.hasSpeed(game.player2Id) shouldBe false
                    genericCostOf(game, "Divination", game.player2Id) shouldBe 2
                }
            }
        }

        context("Samut's lord — other creatures you control get +X/+0") {

            test("scales other creatures with speed and leaves Samut alone") {
                val game = samutGame {
                    withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                }
                val bears = game.findPermanent("Grizzly Bears")!!
                val samut = game.findPermanent("Samut, the Driving Force")!!

                withClue("at speed 1 the 2/2 Bears is a 3/2") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }

                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first
                withClue("at max speed the Bears is a 6/2 — toughness is +0, not +X") {
                    game.state.projectedState.getPower(bears) shouldBe 6
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
                withClue("\"other creatures\" excludes Samut, who stays a printed 4/5") {
                    game.state.projectedState.getPower(samut) shouldBe 4
                    game.state.projectedState.getToughness(samut) shouldBe 5
                }
            }

            test("does not pump creatures an opponent controls") {
                val game = samutGame {
                    withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                }
                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("the lord is \"you control\"-scoped") {
                    game.state.projectedState.getPower(bears) shouldBe 2
                }
            }
        }
    }

    /**
     * Samut on player 1's battlefield, both libraries stocked, arriving at player 1's main phase
     * through a real step sequence — the state-based action that starts speed is polled by the game
     * loop, so building straight into the main phase would leave speed unset.
     */
    private fun samutGame(extra: ScenarioBuilder.() -> Unit): TestGame {
        val builder = scenario().withPlayers("Player1", "Player2")
        repeat(12) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
        builder.withCardOnBattlefield(1, "Samut, the Driving Force", summoningSickness = false)
        builder.extra()
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .build()
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        return game
    }

    private fun genericCostOf(game: TestGame, cardName: String, playerId: EntityId): Int =
        CostCalculator(cardRegistry).calculateEffectiveCost(
            game.state,
            cardRegistry.requireCard(cardName),
            playerId,
        ).genericAmount
}
