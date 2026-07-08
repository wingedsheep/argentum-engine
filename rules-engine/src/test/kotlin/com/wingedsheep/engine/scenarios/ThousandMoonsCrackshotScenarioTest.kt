package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Thousand Moons Crackshot ({1}{W} 2/2 Creature — Human Soldier) — Lost Caverns of Ixalan.
 *
 * "Whenever this creature attacks, you may pay {2}{W}. When you do, tap target creature."
 *
 * Modelled as a `ReflexiveTriggerEffect` whose optional action is the {2}{W} payment: after the
 * attack trigger resolves, the engine offers the optional payment first (yes/no + mana sources),
 * then — only if paid — the reflexive "tap target creature" goes on the stack and asks for its
 * target. Paying taps the chosen creature; declining leaves it untapped.
 */
class ThousandMoonsCrackshotScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
    }

    fun GameTestDriver.isTapped(id: com.wingedsheep.sdk.model.EntityId): Boolean =
        state.getEntity(id)?.get<TappedComponent>() != null

    test("paying {2}{W} taps target creature") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = if (you == d.player1) d.player2 else d.player1

        val crackshot = d.putCreatureOnBattlefield(you, "Thousand Moons Crackshot")
        d.removeSummoningSickness(crackshot)
        // Three Plains provide the {2}{W} for the optional payment.
        repeat(3) { d.putLandOnBattlefield(you, "Plains") }
        val victim = d.putCreatureOnBattlefield(opponent, "Grizzly Bears") // 2/2

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(crackshot), opponent)

        // Answer in the engine's order: pay (yes) -> auto-pay mana sources -> choose target.
        var targeted = false
        var guard = 0
        while (!targeted && guard++ < 40) {
            when (d.pendingDecision) {
                is YesNoDecision -> d.submitYesNo(you, true)
                is SelectManaSourcesDecision -> d.submitManaAutoPayOrDecline(you, true)
                is ChooseTargetsDecision -> {
                    d.submitTargetSelection(you, listOf(victim)); targeted = true
                }
                else -> d.bothPass()
            }
        }
        targeted shouldBe true

        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.isTapped(victim) shouldBe true
    }

    test("declining the {2}{W} payment leaves the creature untapped") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = if (you == d.player1) d.player2 else d.player1

        val crackshot = d.putCreatureOnBattlefield(you, "Thousand Moons Crackshot")
        d.removeSummoningSickness(crackshot)
        repeat(3) { d.putLandOnBattlefield(you, "Plains") }
        val victim = d.putCreatureOnBattlefield(opponent, "Grizzly Bears") // 2/2

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(crackshot), opponent)

        // The pay choice is genuinely offered (mana is available) — decline it. No target is chosen.
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

        d.isTapped(victim) shouldBe false
    }
})
