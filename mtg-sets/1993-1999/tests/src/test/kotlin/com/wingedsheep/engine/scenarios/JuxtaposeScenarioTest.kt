package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Juxtapose — {3}{U} Sorcery (Legends).
 *
 * "You and target player exchange control of the creature you each control with the greatest mana
 * value. Then exchange control of artifacts the same way. If two or more permanents a player controls
 * are tied for greatest, their controller chooses one of them."
 */
class JuxtaposeScenarioTest : ScenarioTestBase() {
    init {
        context("Juxtapose") {
            test("swaps each side's greatest creature, then greatest artifact; the target player breaks their own tie") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Juxtapose")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(1, "Sol Ring")
                    .withCardOnBattlefield(1, "Icy Manipulator")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withCardOnBattlefield(2, "Air Elemental")
                    .withCardOnBattlefield(2, "Millstone")
                    .withCardOnBattlefield(2, "Jayemdae Tome")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                fun controllerOf(name: String) = game.state.projectedState.getController(game.findPermanent(name)!!)

                game.castSpellTargetingPlayer(1, "Juxtapose", 2).error shouldBe null
                game.resolveStack()

                // P1's Craw Wurm is their sole greatest creature, so only P2 is asked — between the two 5s.
                val tie = game.getPendingDecision() as SelectCardsDecision
                withClue("the target player breaks their own tie") { tie.playerId shouldBe game.player2Id }
                withClue("only the creatures tied for greatest are offered") {
                    tie.options.toSet() shouldBe
                        setOf(game.findPermanent("Serra Angel")!!, game.findPermanent("Air Elemental")!!)
                }
                game.selectCards(listOf(game.findPermanent("Air Elemental")!!))
                game.resolveStack()

                controllerOf("Craw Wurm") shouldBe game.player2Id
                controllerOf("Air Elemental") shouldBe game.player1Id
                controllerOf("Serra Angel") shouldBe game.player2Id
                controllerOf("Grizzly Bears") shouldBe game.player1Id
                controllerOf("Hill Giant") shouldBe game.player2Id

                controllerOf("Icy Manipulator") shouldBe game.player2Id
                controllerOf("Jayemdae Tome") shouldBe game.player1Id
                controllerOf("Sol Ring") shouldBe game.player1Id
                controllerOf("Millstone") shouldBe game.player2Id
            }

            test("with no creature on one side the creature exchange does nothing but artifacts still swap") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Juxtapose")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(1, "Sol Ring")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Millstone")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                fun controllerOf(name: String) = game.state.projectedState.getController(game.findPermanent(name)!!)

                game.castSpellTargetingPlayer(1, "Juxtapose", 2).error shouldBe null
                game.resolveStack()

                controllerOf("Grizzly Bears") shouldBe game.player2Id
                controllerOf("Sol Ring") shouldBe game.player2Id
                controllerOf("Millstone") shouldBe game.player1Id
            }

            test("an artifact creature swapped as a creature is weighed for its new controller in the artifact exchange") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Juxtapose")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(1, "Juggernaut")
                    .withCardOnBattlefield(1, "Sol Ring")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Millstone")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                fun controllerOf(name: String) = game.state.projectedState.getController(game.findPermanent(name)!!)

                game.castSpellTargetingPlayer(1, "Juxtapose", 2).error shouldBe null
                game.resolveStack()

                // Creatures: Juggernaut <-> Grizzly Bears. Then P2's greatest artifact is the
                // Juggernaut they now control, which goes back for P1's Sol Ring.
                controllerOf("Grizzly Bears") shouldBe game.player1Id
                controllerOf("Juggernaut") shouldBe game.player1Id
                controllerOf("Sol Ring") shouldBe game.player2Id
                controllerOf("Millstone") shouldBe game.player2Id
            }
        }
    }
}
