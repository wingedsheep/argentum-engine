package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Taunting Arbormage (ZNR #212) — {2}{G} Creature — Elf Wizard, 2/3.
 *
 *   Kicker {3}
 *   When this creature enters, if it was kicked, all creatures able to block target creature this
 *   turn do so.
 */
class TauntingArbormageScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Taunting Arbormage")
        .withLandsOnBattlefield(1, "Forest", 6)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Savannah Lions")
        .withCardOnBattlefield(2, "Elvish Warrior")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.cast(kicked: Boolean) {
        val cardId = findCardsInHand(1, "Taunting Arbormage").single()
        val actions = getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == cardId }
        val chosen = actions.single { it.actionType == if (kicked) "CastWithKicker" else "CastSpell" }
        execute(chosen.action).error shouldBe null
        resolveStack()
    }

    init {
        test("kicked: every creature able to block the target must block it this turn") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.cast(kicked = true)

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            withClue("no blocks is illegal while defenders can block the taunted creature") {
                game.declareNoBlockers().outcome shouldNotBe Outcome.Done
            }
            withClue("blocking the other attacker instead is illegal") {
                game.declareBlockers(
                    mapOf("Savannah Lions" to listOf("Grizzly Bears"), "Elvish Warrior" to listOf("Hill Giant"))
                ).outcome shouldNotBe Outcome.Done
            }
            withClue("both defenders blocking the taunted creature is legal") {
                game.declareBlockers(
                    mapOf("Savannah Lions" to listOf("Grizzly Bears"), "Elvish Warrior" to listOf("Grizzly Bears"))
                ).outcome shouldBe Outcome.Done
            }
        }

        test("unkicked: the trigger does nothing") {
            val game = board()
            game.cast(kicked = false)

            withClue("no target prompt and nothing on the stack") {
                game.getPendingDecision() shouldBe null
                game.state.stack.isEmpty() shouldBe true
            }

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().outcome shouldBe Outcome.Done
        }
    }
}
