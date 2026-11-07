package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Frenzied Goblin ({R} 1/1 Creature — Goblin Berserker) — Ravnica: City of Guilds.
 *
 * "Whenever this creature attacks, you may pay {R}. If you do, target creature can't block
 *  this turn."
 *
 * Modelled with `Effects.MayPay` (the Lightning Rift shape): the creature target is chosen as the
 * trigger goes on the stack (CR 603.3d), and the optional {R} is offered only as it resolves. Paying
 * applies the can't-block restriction to the target; declining leaves it able to block.
 */
class FrenziedGoblinScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
    }

    test("paying {R} makes the target creature unable to block this turn") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = if (you == d.player1) d.player2 else d.player1

        val goblin = d.putCreatureOnBattlefield(you, "Frenzied Goblin")
        d.removeSummoningSickness(goblin)
        d.putLandOnBattlefield(you, "Mountain") // taps for the optional {R}
        val blocker = d.putCreatureOnBattlefield(opponent, "Grizzly Bears") // 2/2

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(goblin), opponent)

        // Advance priority until each decision surfaces, answering in the engine's order:
        // choose the creature target (trigger goes on the stack) → pay {R} (yes) → mana sources.
        var targeted = false
        var paid = false
        var guard = 0
        while (!paid && guard++ < 40) {
            when (d.pendingDecision) {
                is ChooseTargetsDecision -> {
                    d.submitTargetSelection(you, listOf(blocker)); targeted = true
                }
                is YesNoDecision -> {
                    targeted shouldBe true // the target is locked before the pay question
                    d.submitYesNo(you, true)
                }
                is SelectManaSourcesDecision -> {
                    d.submitManaAutoPayOrDecline(you, true); paid = true
                }
                else -> d.bothPass()
            }
        }
        paid shouldBe true

        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.state.projectedState.cantBlock(blocker) shouldBe true
    }

    test("declining the {R} payment leaves the creature able to block") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = if (you == d.player1) d.player2 else d.player1

        val goblin = d.putCreatureOnBattlefield(you, "Frenzied Goblin")
        d.removeSummoningSickness(goblin)
        d.putLandOnBattlefield(you, "Mountain")
        val blocker = d.putCreatureOnBattlefield(opponent, "Grizzly Bears") // 2/2

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(goblin), opponent)

        // The pay-{R} choice is genuinely offered (mana is available) — but decline it. The target
        // was already chosen; declining means the "if you do" restriction never applies.
        var answered = false
        var guard = 0
        while (!answered && guard++ < 40) {
            when (d.pendingDecision) {
                is ChooseTargetsDecision -> d.submitTargetSelection(you, listOf(blocker))
                is YesNoDecision -> {
                    d.submitYesNo(you, false); answered = true
                }
                else -> d.bothPass()
            }
        }
        answered shouldBe true

        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.state.projectedState.cantBlock(blocker) shouldBe false
    }
})
