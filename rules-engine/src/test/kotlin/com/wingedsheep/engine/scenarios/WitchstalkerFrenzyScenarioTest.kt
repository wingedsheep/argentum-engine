package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Witchstalker Frenzy's cost reduction — i.e. for the new
 * `CostReductionSource.CreaturesThatAttackedThisTurn`.
 *
 * Oracle: "This spell costs {1} less to cast for each creature that attacked this turn."
 *
 * The source counts turn *history* (the union of every player's `PlayerAttackersThisTurnComponent`),
 * not the live battlefield, and is not controller-scoped. The two tests that matter are therefore
 * the dead attacker and the opposing attacker: a battlefield-scanning implementation would pass the
 * plain "two attackers" case and silently fail both of those.
 */
class WitchstalkerFrenzyScenarioTest : ScenarioTestBase() {

    init {
        context("Witchstalker Frenzy — costs {1} less per creature that attacked this turn") {

            test("no creature attacked → full {3}{R} (3 generic)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Witchstalker Frenzy")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cost = CostCalculator(cardRegistry).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Witchstalker Frenzy"),
                    game.player1Id,
                )

                withClue("nothing attacked, so the generic component stays at 3") {
                    cost.genericAmount shouldBe 3
                }
            }

            test("two creatures attacked → {1}{R} (1 generic)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Witchstalker Frenzy")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", tapped = false, summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)
                ).error shouldBe null

                val cost = CostCalculator(cardRegistry).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Witchstalker Frenzy"),
                    game.player1Id,
                )

                withClue("two attackers reduce the generic from 3 to 1") {
                    cost.genericAmount shouldBe 1
                }
            }

            test("an attacker that died in combat still counts") {
                // Grizzly Bears (2/2) attacks into Hill Giant (3/3) and dies in the damage step.
                // The card is a combat trick cast *after* damage, so this is the case that matters.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Witchstalker Frenzy")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", tapped = false, summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the attacker traded with the blocker and is in the graveyard") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }

                val cost = CostCalculator(cardRegistry).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Witchstalker Frenzy"),
                    game.player1Id,
                )

                withClue("a creature that attacked and died still discounts the spell") {
                    cost.genericAmount shouldBe 2
                }
            }

            test("an opponent's attackers discount the defending player's copy too") {
                // "each creature that attacked this turn" is not controller-scoped: player 1
                // attacks, player 2 holds the Frenzy.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Witchstalker Frenzy")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null

                val cost = CostCalculator(cardRegistry).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Witchstalker Frenzy"),
                    game.player2Id,
                )

                withClue("the attacking player's creature discounts the defender's spell") {
                    cost.genericAmount shouldBe 2
                }
            }

            test("the reduction never eats the colored pip") {
                // Five attackers vs a {3}{R} spell: generic bottoms out at 0, {R} survives.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Witchstalker Frenzy")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(1, "Savannah Lions", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(1, "Centaur Courser", tapped = false, summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf(
                        "Grizzly Bears" to 2,
                        "Hill Giant" to 2,
                        "Savannah Lions" to 2,
                        "Centaur Courser" to 2,
                    )
                ).error shouldBe null

                val cost = CostCalculator(cardRegistry).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Witchstalker Frenzy"),
                    game.player1Id,
                )

                withClue("four attackers clear all 3 generic but cannot reduce below {R}") {
                    cost.genericAmount shouldBe 0
                    cost.colorCount[Color.RED] shouldBe 1
                }
            }
        }
    }
}
