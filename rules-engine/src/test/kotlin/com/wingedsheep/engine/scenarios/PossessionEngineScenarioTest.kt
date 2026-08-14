package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Possession Engine (DFT #54).
 *
 * Possession Engine {3}{U}{U} — Artifact — Vehicle 5/5
 * When this Vehicle enters, gain control of target creature an opponent controls for as long as
 * you control this Vehicle. That creature can't attack or block for as long as you control this
 * Vehicle.
 * Crew 3
 *
 * The load-bearing claim is the shared `WhileYouControlSource` duration: the steal *and* the
 * combat lock must both end the moment the Vehicle stops being yours, rather than lingering (the
 * `WhileSourceOnBattlefield` failure mode) or expiring at cleanup (the `EndOfTurn` default on
 * `Effects.CantAttackOrBlock`, which would silently free the creature after one turn).
 */
class PossessionEngineScenarioTest : ScenarioTestBase() {

    init {
        context("Possession Engine") {

            test("the enters trigger steals the creature and locks it out of combat") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Possession Engine")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("the opponent starts with their own creature") {
                    game.state.getEntity(bears)!!.get<ControllerComponent>()!!.playerId shouldBe game.player2Id
                }

                game.castSpell(1, "Possession Engine").error shouldBe null
                game.resolveStack()

                withClue("the enters trigger should pause to choose a target") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("control moved to the Vehicle's controller") {
                    projected.getController(bears) shouldBe game.player1Id
                }
                withClue("and the creature can neither attack nor block") {
                    projected.cantAttack(bears) shouldBe true
                    projected.cantBlock(bears) shouldBe true
                }
            }

            test("both halves last past end of turn — the lock is not an until-end-of-turn effect") {
                val game = stealGame()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                val projected = game.state.projectedState
                withClue("cleanup must not undo a 'for as long as you control this Vehicle' effect") {
                    projected.getController(bears) shouldBe game.player1Id
                    projected.cantAttack(bears) shouldBe true
                    projected.cantBlock(bears) shouldBe true
                }
            }

            test("destroying the Vehicle hands the creature back and lifts the combat lock") {
                val game = stealGame(extraLands = 2)
                val bears = game.findPermanent("Grizzly Bears")!!
                val engine = game.findPermanent("Possession Engine")!!

                // Skycrash (DFT) — "Destroy target artifact." Aimed at our own Vehicle, which is
                // the cheapest way to make "you control this Vehicle" stop being true.
                game.castSpell(1, "Skycrash", engine).error shouldBe null
                game.resolveStack()

                withClue("the Vehicle is gone") {
                    game.isOnBattlefield("Possession Engine") shouldBe false
                }

                val projected = game.state.projectedState
                withClue("the duration ended, so control reverts to the owner") {
                    projected.getController(bears) shouldBe game.player2Id
                }
                withClue("and the creature can attack and block again") {
                    projected.cantAttack(bears) shouldBe false
                    projected.cantBlock(bears) shouldBe false
                }
            }
        }
    }

    /** A game in which Possession Engine has already resolved and stolen the opposing Grizzly Bears. */
    private fun stealGame(extraLands: Int = 0): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Possession Engine")
            .withLandsOnBattlefield(1, "Island", 5)
            .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
        if (extraLands > 0) {
            builder.withCardInHand(1, "Skycrash")
            builder.withLandsOnBattlefield(1, "Mountain", extraLands)
        }
        repeat(6) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val bears = game.findPermanent("Grizzly Bears")!!
        game.castSpell(1, "Possession Engine")
        game.resolveStack()
        game.selectTargets(listOf(bears))
        game.resolveStack()
        return game
    }
}
