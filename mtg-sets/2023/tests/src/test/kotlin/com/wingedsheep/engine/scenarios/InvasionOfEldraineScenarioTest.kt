package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Invasion of Eldraine // Prickle Faeries.
 *
 * Front: target opponent discards two cards. Back: a 2/2 flier that deals 2 damage to each
 * opponent at the beginning of their upkeep if they have two or fewer cards in hand.
 */
class InvasionOfEldraineScenarioTest : ScenarioTestBase() {

    private fun TestGame.defeatSiegeAndCastBack() {
        checkStateBasedActions()
        advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackersWithPermanentTargets(
            permanentAttackers = mapOf("Serra Angel" to "Invasion of Eldraine")
        ).error shouldBe null
        var guard = 0
        while (state.pendingDecision == null && guard++ < 30) {
            if (state.step == Step.DECLARE_BLOCKERS &&
                state.getEntity(player2Id)
                    ?.has<com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent>() != true
            ) {
                declareNoBlockers()
            } else {
                passPriority()
            }
        }
        answerYesNo(true).error shouldBe null
        resolveStack()
    }

    private fun backFaceGame(extra: ScenarioBuilder.() -> ScenarioBuilder) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Invasion of Eldraine")
        .withCardOnBattlefield(1, "Serra Angel", summoningSickness = false)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .extra()
        .build()

    init {
        context("front face — Invasion of Eldraine") {
            test("target opponent discards two cards") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Invasion of Eldraine")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Forest")
                    .withCardInHand(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Invasion of Eldraine").error shouldBe null
                // The lone opponent is auto-targeted; they choose which two cards to discard.
                game.resolveStack()
                val hand = listOfNotNull(
                    game.findCardsInHand(2, "Grizzly Bears").firstOrNull(),
                    game.findCardsInHand(2, "Forest").firstOrNull(),
                )
                game.selectCards(hand).error shouldBe null
                game.resolveStack()

                withClue("the opponent discards exactly two of their three cards") {
                    game.handSize(2) shouldBe 1
                }
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Forest") shouldBe true
            }
        }

        context("back face — Prickle Faeries") {
            test("deals 2 damage at the opponent's upkeep when they hold two or fewer cards") {
                val game = backFaceGame { withCardInHand(2, "Forest").withCardInHand(2, "Island") }

                game.defeatSiegeAndCastBack()
                game.isOnBattlefield("Prickle Faeries") shouldBe true
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
            }

            test("no damage when the opponent holds three or more cards") {
                val game = backFaceGame {
                    withCardInHand(2, "Forest").withCardInHand(2, "Island").withCardInHand(2, "Plains")
                }

                game.defeatSiegeAndCastBack()
                game.isOnBattlefield("Prickle Faeries") shouldBe true
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
