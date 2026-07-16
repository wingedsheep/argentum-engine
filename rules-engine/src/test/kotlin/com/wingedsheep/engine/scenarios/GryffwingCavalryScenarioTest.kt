package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Gryffwing Cavalry (VOW #16) — {3}{W} 2/2 Creature — Human Knight, Flying + Training + a reflexive
 * flying-granter.
 *
 * "Whenever this creature attacks, you may pay {1}{W}. If you do, target attacking creature without
 * flying gains flying until end of turn." Modelled as [MayPayManaEffect] (the "you may pay … if you
 * do" reflexive, CR 603.12): after the attack trigger resolves the engine offers the optional
 * payment first (yes/no + mana sources), then — only if paid — the "gains flying" effect targets a
 * grounded attacker. The target filter is `AttackingCreature.withoutKeyword(FLYING)`, so only a
 * grounded attacker is legal and the Cavalry (which flies) is never a legal target for its own
 * trigger.
 *
 * To isolate the reflexive from Training, the Cavalry (power 2) attacks alongside a lower-power
 * creature (Savannah Lions, power 1) — Training does not fire, so only the reflexive is on the
 * stack.
 */
class GryffwingCavalryScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
    }

    test("paying {1}{W} grants flying to a grounded attacker") {
        val d = driver()
        val you = d.activePlayer!!
        val opp = d.getOpponent(you)

        val cavalry = d.putCreatureOnBattlefield(you, "Gryffwing Cavalry")  // 2/2 flying
        val lions = d.putCreatureOnBattlefield(you, "Savannah Lions")       // 1/1, grounded, power 1
        listOf(cavalry, lions).forEach { d.removeSummoningSickness(it) }
        repeat(2) { d.putLandOnBattlefield(you, "Plains") }                 // {1}{W} for the optional payment

        d.state.projectedState.hasKeyword(lions, Keyword.FLYING) shouldBe false

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(cavalry, lions), opp)

        // Engine order: pay (yes) -> auto-pay mana sources -> choose the grounded attacker.
        var targeted = false
        var guard = 0
        while (!targeted && guard++ < 40) {
            when (d.pendingDecision) {
                is YesNoDecision -> d.submitYesNo(you, true)
                is SelectManaSourcesDecision -> d.submitManaAutoPayOrDecline(you, true)
                is ChooseTargetsDecision -> {
                    d.submitTargetSelection(you, listOf(lions)); targeted = true
                }
                else -> d.bothPass()
            }
        }
        targeted shouldBe true
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.state.projectedState.hasKeyword(lions, Keyword.FLYING) shouldBe true
    }

    test("declining the {1}{W} payment leaves the grounded attacker without flying") {
        val d = driver()
        val you = d.activePlayer!!
        val opp = d.getOpponent(you)

        val cavalry = d.putCreatureOnBattlefield(you, "Gryffwing Cavalry")
        val lions = d.putCreatureOnBattlefield(you, "Savannah Lions")
        listOf(cavalry, lions).forEach { d.removeSummoningSickness(it) }
        repeat(2) { d.putLandOnBattlefield(you, "Plains") }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(cavalry, lions), opp)

        var answered = false
        var guard = 0
        while (!answered && guard++ < 40) {
            when (d.pendingDecision) {
                is YesNoDecision -> {
                    d.submitYesNo(you, false); answered = true
                }
                else -> d.bothPass()
            }
        }
        answered shouldBe true
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.state.projectedState.hasKeyword(lions, Keyword.FLYING) shouldBe false
    }
})
