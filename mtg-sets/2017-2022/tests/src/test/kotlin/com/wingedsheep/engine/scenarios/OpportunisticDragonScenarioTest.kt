package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Opportunistic Dragon (ELD #133).
 *
 * Opportunistic Dragon {2}{R}{R} — Creature — Dragon 4/3
 * Flying
 * When this creature enters, choose target Human or artifact an opponent controls. For as long as
 * this creature remains on the battlefield, gain control of that permanent, it loses all
 * abilities, and it can't attack or block.
 *
 * The load-bearing claim is the shared `WhileSourceOnBattlefield` duration: the steal, the ability
 * loss and the combat lock must all outlive cleanup (not the `EndOfTurn` defaults on the
 * facades) and all end together the moment the Dragon leaves.
 */
class OpportunisticDragonScenarioTest : ScenarioTestBase() {

    init {
        context("Opportunistic Dragon") {

            test("steals a Human, strips its abilities and locks it out of combat past end of turn") {
                val game = stealGame()
                val knight = game.findPermanent("Youthful Knight")!!

                val projected = game.state.projectedState
                withClue("control moved to the Dragon's controller") {
                    projected.getController(knight) shouldBe game.player1Id
                }
                withClue("the Human lost its abilities") {
                    projected.hasKeyword(knight, Keyword.FIRST_STRIKE) shouldBe false
                }
                withClue("and it can neither attack nor block") {
                    projected.cantAttack(knight) shouldBe true
                    projected.cantBlock(knight) shouldBe true
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                val later = game.state.projectedState
                withClue("cleanup must not end a 'for as long as this creature remains' effect") {
                    later.getController(knight) shouldBe game.player1Id
                    later.hasKeyword(knight, Keyword.FIRST_STRIKE) shouldBe false
                    later.cantAttack(knight) shouldBe true
                    later.cantBlock(knight) shouldBe true
                }
            }

            test("when the Dragon leaves, the Human goes home with its abilities and can fight again") {
                val game = stealGame(withRemoval = true)
                val knight = game.findPermanent("Youthful Knight")!!
                val dragon = game.findPermanent("Opportunistic Dragon")!!

                game.castSpell(1, "Scorching Dragonfire", dragon).error shouldBe null
                game.resolveStack()

                withClue("the Dragon is gone") {
                    game.isOnBattlefield("Opportunistic Dragon") shouldBe false
                }

                val projected = game.state.projectedState
                withClue("control reverts to the owner") {
                    projected.getController(knight) shouldBe game.player2Id
                }
                withClue("its abilities return") {
                    projected.hasKeyword(knight, Keyword.FIRST_STRIKE) shouldBe true
                }
                withClue("and the combat lock is lifted") {
                    projected.cantAttack(knight) shouldBe false
                    projected.cantBlock(knight) shouldBe false
                }
            }
        }
    }

    /** A game in which Opportunistic Dragon has resolved and stolen the opposing Youthful Knight. */
    private fun stealGame(withRemoval: Boolean = false): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Opportunistic Dragon")
            .withLandsOnBattlefield(1, "Mountain", if (withRemoval) 6 else 4)
            .withCardOnBattlefield(2, "Youthful Knight", summoningSickness = false)
        if (withRemoval) builder.withCardInHand(1, "Scorching Dragonfire")
        repeat(6) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val knight = game.findPermanent("Youthful Knight")!!
        game.castSpell(1, "Opportunistic Dragon").error shouldBe null
        game.resolveStack()
        game.selectTargets(listOf(knight)).error shouldBe null
        game.resolveStack()
        return game
    }
}
