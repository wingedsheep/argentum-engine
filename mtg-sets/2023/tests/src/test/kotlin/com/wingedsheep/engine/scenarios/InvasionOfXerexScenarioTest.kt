package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Invasion of Xerex // Vertex Paladin.
 *
 * Front: "return up to one target creature to its owner's hand" — optional target, so zero targets
 * is legal. Back: a flying `*`/`*` equal to the number of creatures you control.
 */
class InvasionOfXerexScenarioTest : ScenarioTestBase() {

    private fun xerexGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Invasion of Xerex")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("front face — the enters trigger") {

            test("returns the targeted creature to its owner's hand") {
                val game = xerexGame()
                game.castSpell(1, "Invasion of Xerex").error shouldBe null
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()

                game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Invasion of Xerex") shouldBe true
            }

            test("up to one — choosing no target leaves everything in place") {
                val game = xerexGame()
                game.castSpell(1, "Invasion of Xerex").error shouldBe null
                game.resolveStack()
                if (game.getPendingDecision() is ChooseTargetsDecision) game.skipTargets().error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.state.stack.isEmpty() shouldBe true
            }
        }

        context("back face — Vertex Paladin") {

            test("flying, and its power and toughness each equal the number of creatures you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Invasion of Xerex")
                    .withCardOnBattlefield(1, "Shivan Dragon", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.checkStateBasedActions()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Shivan Dragon" to "Invasion of Xerex")
                ).error shouldBe null
                var guard = 0
                while (game.state.pendingDecision == null && guard++ < 30) {
                    if (game.state.step == Step.DECLARE_BLOCKERS &&
                        game.state.getEntity(game.player2Id)
                            ?.has<com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent>() != true
                    ) {
                        game.declareNoBlockers()
                    } else {
                        game.passPriority()
                    }
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                val paladin = game.findPermanent("Vertex Paladin")!!
                withClue("Shivan Dragon, Grizzly Bears and the Paladin itself — the opponent's creatures don't count") {
                    game.state.projectedState.getPower(paladin) shouldBe 3
                    game.state.projectedState.getToughness(paladin) shouldBe 3
                }
                game.state.projectedState.hasKeyword(paladin, com.wingedsheep.sdk.core.Keyword.FLYING) shouldBe true
            }
        }
    }
}
