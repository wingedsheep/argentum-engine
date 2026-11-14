package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Ravaging Blaze (ORI #159).
 *
 * "Ravaging Blaze deals X damage to target creature.
 *  Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard,
 *  Ravaging Blaze also deals X damage to that creature's controller."
 *
 * The controller damage goes to whoever controls the targeted creature (not necessarily an
 * opponent), spell mastery is checked as the spell resolves, and if the creature is gone by then
 * the spell doesn't resolve — no damage to the creature or its controller.
 */
class RavagingBlazeScenarioTest : ScenarioTestBase() {

    private fun battle(
        casterGraveyard: List<String> = emptyList(),
        extraInHand: List<String> = emptyList(),
        ownCreature: Boolean = false,
    ): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Ravaging Blaze")
            .withLandsOnBattlefield(1, "Mountain", 5)
            .withCardOnBattlefield(if (ownCreature) 1 else 2, "Hill Giant")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        casterGraveyard.forEach { builder.withCardInGraveyard(1, it) }
        extraInHand.forEach { builder.withCardInHand(1, it) }
        return builder.build()
    }

    private fun TestGame.blazeTheGiant(x: Int) {
        val giant = findPermanent("Hill Giant")!!
        castXSpell(1, "Ravaging Blaze", x, giant).error shouldBe null
    }

    init {
        context("Ravaging Blaze") {

            test("without spell mastery it deals X damage to the creature only") {
                val game = battle()
                game.blazeTheGiant(3)
                game.resolveStack()

                withClue("3 damage kills Hill Giant (3/3)") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                game.getLifeTotal(2) shouldBe 20
                game.getLifeTotal(1) shouldBe 20
            }

            test("spell mastery: it also deals X damage to the creature's controller") {
                val game = battle(casterGraveyard = listOf("Lightning Bolt", "Divination"))
                game.blazeTheGiant(3)
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe false
                withClue("the giant's controller takes X = 3") {
                    game.getLifeTotal(2) shouldBe 17
                }
                game.getLifeTotal(1) shouldBe 20
            }

            test("the controller damage follows the creature — your own creature hits you") {
                val game = battle(casterGraveyard = listOf("Lightning Bolt", "Divination"), ownCreature = true)
                game.blazeTheGiant(2)
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe true
                game.getLifeTotal(1) shouldBe 18
                game.getLifeTotal(2) shouldBe 20
            }

            test("one instant in your graveyard isn't enough — Ravaging Blaze doesn't count itself") {
                val game = battle(casterGraveyard = listOf("Lightning Bolt"))
                game.blazeTheGiant(3)
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe false
                game.getLifeTotal(2) shouldBe 20
            }

            test("if the creature is gone on resolution, neither the creature nor its controller is dealt damage") {
                // Spell mastery would apply: Divination is already in the graveyard and the Bolt
                // cast in response joins it before Ravaging Blaze resolves.
                val game = battle(casterGraveyard = listOf("Divination"), extraInHand = listOf("Lightning Bolt"))
                game.blazeTheGiant(2)
                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Lightning Bolt", giant).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isInGraveyard(1, "Ravaging Blaze") shouldBe true
                withClue("Ravaging Blaze fizzled, so the giant's controller took no damage") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
