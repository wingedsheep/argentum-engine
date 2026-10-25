package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Venerable Warsinger (STX #246, {1}{R}{W}, Creature — Spirit Cleric 3/3).
 *
 *   Vigilance, trample
 *   Whenever this creature deals combat damage to a player, you may return target creature card
 *   with mana value X or less from your graveyard to the battlefield, where X is the amount of
 *   damage this creature dealt to that player.
 *
 * X is the damage dealt to the player, known when the trigger's target is chosen: the legal targets
 * are capped by it (3 unblocked, 2 when trample pushes only 2 past a 1/1 blocker), not by the
 * Warsinger's power and not by zero.
 */
class VenerableWarsingerScenarioTest : ScenarioTestBase() {

    /** Push the declared combat through to the Warsinger's trigger target prompt. */
    private fun TestGame.finishCombat() {
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        resolveStack()
        if (hasPendingDecision() && getPendingDecision() !is ChooseTargetsDecision) {
            submitDefaultCombatDamage()
            resolveStack()
        }
    }

    private fun TestGame.attackWithWarsinger() {
        advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        withClue("Venerable Warsinger should be able to attack") {
            declareAttackers(mapOf("Venerable Warsinger" to 2)).error shouldBe null
        }
    }

    private fun graveyardScenario() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Venerable Warsinger", summoningSickness = false)
        .withCardInGraveyard(1, "Grizzly Bears") // MV 2
        .withCardInGraveyard(1, "Gray Ogre")     // MV 3
        .withCardInGraveyard(1, "Hill Giant")    // MV 4
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Venerable Warsinger") {

            test("unblocked, 3 damage lets it return a creature card with mana value 3 or less") {
                val game = graveyardScenario().build()
                game.attackWithWarsinger()
                game.finishCombat()

                game.getLifeTotal(2) shouldBe 17
                val decision = game.getPendingDecision()
                withClue("The trigger should ask for a target; got $decision") {
                    decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                }
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val ogre = game.findCardsInGraveyard(1, "Gray Ogre").single()
                withClue("Only creature cards with mana value 3 or less are legal") {
                    (decision as ChooseTargetsDecision).legalTargets[0].orEmpty()
                        .shouldContainExactlyInAnyOrder(bears, ogre)
                }

                game.selectTargets(listOf(ogre)).error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) game.answerYesNo(true)

                withClue("Gray Ogre should be on the battlefield under Alice's control") {
                    game.isOnBattlefield("Gray Ogre") shouldBe true
                    game.isInGraveyard(1, "Gray Ogre") shouldBe false
                }
            }

            test("trampling over a 1/1 deals 2 to the player, so X is 2") {
                val game = graveyardScenario()
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .build()
                game.attackWithWarsinger()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Llanowar Elves" to listOf("Venerable Warsinger"))).error shouldBe null
                game.finishCombat()

                withClue("Trample should carry 2 of the 3 damage to Bob") {
                    game.getLifeTotal(2) shouldBe 18
                }
                val decision = game.getPendingDecision()
                withClue("The trigger should ask for a target; got $decision") {
                    decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                }
                val legal = (decision as ChooseTargetsDecision).legalTargets[0].orEmpty()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val ogre = game.findCardsInGraveyard(1, "Gray Ogre").single()
                withClue("X is the damage dealt to the player (2), not the Warsinger's power") {
                    legal shouldContain bears
                    legal shouldNotContain ogre
                }
            }

            test("declining the may leaves the card in the graveyard") {
                val game = graveyardScenario().build()
                game.attackWithWarsinger()
                game.finishCombat()

                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(emptyList()).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Gray Ogre") shouldBe true
            }
        }
    }
}
