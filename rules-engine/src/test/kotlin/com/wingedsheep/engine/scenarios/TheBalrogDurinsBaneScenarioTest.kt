package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Balrog, Durin's Bane (LTR #195) — {5}{B}{R} Legendary Creature — Avatar Demon, 7/5.
 *
 *   This spell costs {1} less to cast for each permanent sacrificed this turn.
 *   Haste
 *   The Balrog can't be blocked except by legendary creatures.
 *   When The Balrog dies, destroy target artifact or creature an opponent controls.
 *
 * Exercises the new SDK primitive
 * [com.wingedsheep.sdk.scripting.CostReductionSource.PermanentsSacrificedThisTurn] and its
 * backing turn-scoped counter `GameState.permanentsSacrificedThisTurn` (incremented by the
 * central sacrifice hook `ZoneTransitionService.trackPermanentSacrifice`, reset each turn).
 */
class TheBalrogDurinsBaneScenarioTest : ScenarioTestBase() {

    private val calculator = CostCalculator(cardRegistry)
    private val balrog get() = cardRegistry.requireCard("The Balrog, Durin's Bane")

    init {
        context("cost reduction by permanents sacrificed this turn") {

            test("base cost is {5}{B}{R} = 7 mana with no sacrifices") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "The Balrog, Durin's Bane")
                    .build()
                val cost = calculator.calculateEffectiveCost(game.state, balrog, game.player1Id)
                cost.cmc shouldBe 7
                cost.genericAmount shouldBe 5
            }

            test("each permanent sacrificed this turn reduces the cost by {1}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "The Balrog, Durin's Bane")
                    .build()

                // Three permanents sacrificed this turn → {3} less → {2}{B}{R} = 4 mana.
                game.state = game.state.copy(permanentsSacrificedThisTurn = 3)
                val cost = calculator.calculateEffectiveCost(game.state, balrog, game.player1Id)
                withClue("{5}{B}{R} reduced by {3} = {2}{B}{R}") {
                    cost.cmc shouldBe 4
                    cost.genericAmount shouldBe 2
                }
            }

            test("the count never reduces the cost below the colored pips") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "The Balrog, Durin's Bane")
                    .build()

                // Far more sacrifices than generic mana — floored at the {B}{R} requirement.
                game.state = game.state.copy(permanentsSacrificedThisTurn = 99)
                val cost = calculator.calculateEffectiveCost(game.state, balrog, game.player1Id)
                withClue("Cannot reduce below the colored {B}{R} requirement") {
                    cost.cmc shouldBe 2
                    cost.genericAmount shouldBe 0
                }
            }

            test("a real in-engine sacrifice increments the per-turn counter and discounts the cast") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .withCardInHand(1, "Nasty End")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .build()

                game.state.permanentsSacrificedThisTurn shouldBe 0

                // Nasty End: sacrifice a creature as an additional cost, then draw.
                val result = game.castSpellWithAdditionalSacrifice(1, "Nasty End", "Grizzly Bears")
                result.error shouldBe null
                game.resolveStack()

                withClue("Sacrificing Grizzly Bears should bump permanentsSacrificedThisTurn to 1") {
                    game.state.permanentsSacrificedThisTurn shouldBe 1
                }
                game.isInGraveyard(1, "Grizzly Bears").shouldBeTrue()

                // The Balrog now costs {1} less → {4}{B}{R} = 6 mana.
                val cost = calculator.calculateEffectiveCost(game.state, balrog, game.player1Id)
                cost.cmc shouldBe 6
            }
        }

        context("haste") {
            test("The Balrog has haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Balrog, Durin's Bane")
                    .build()
                val balrogId = game.findPermanent("The Balrog, Durin's Bane")!!
                game.state.projectedState.hasKeyword(balrogId, Keyword.HASTE).shouldBeTrue()
            }
        }

        context("can't be blocked except by legendary creatures") {

            test("a nonlegendary creature cannot block The Balrog, but a legendary can") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Balrog, Durin's Bane")
                    .withCardOnBattlefield(2, "Grizzly Bears")   // nonlegendary
                    .withCardOnBattlefield(2, "Bill the Pony")    // legendary 1/4
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Forest")
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("The Balrog, Durin's Bane" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("Grizzly Bears is nonlegendary, so it cannot block The Balrog") {
                    game.declareBlockers(
                        mapOf("Grizzly Bears" to listOf("The Balrog, Durin's Bane")),
                    ).error shouldNotBe null
                }

                withClue("Bill the Pony is legendary, so it may block The Balrog") {
                    game.declareBlockers(
                        mapOf("Bill the Pony" to listOf("The Balrog, Durin's Bane")),
                    ).error shouldBe null
                }
            }
        }

        context("dies trigger") {
            test("destroys a target artifact or creature an opponent controls") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .withCardOnBattlefield(1, "The Balrog, Durin's Bane")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(2, "Murder")
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Swamp")
                    .build()

                val balrogId = game.findPermanent("The Balrog, Durin's Bane")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Player 2 Murders The Balrog (player 1's) → it dies, its dies trigger goes on the stack.
                game.castSpell(2, "Murder", balrogId).error shouldBe null
                game.resolveStack()

                // The Balrog's controller is player 1, so "an opponent controls" = player 2's Grizzly Bears.
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(bearsId))
                }
                game.resolveStack()

                withClue("The dies trigger should destroy a creature an opponent controls") {
                    game.isInGraveyard(1, "The Balrog, Durin's Bane").shouldBeTrue()
                    game.isInGraveyard(2, "Grizzly Bears").shouldBeTrue()
                }
            }
        }
    }
}
