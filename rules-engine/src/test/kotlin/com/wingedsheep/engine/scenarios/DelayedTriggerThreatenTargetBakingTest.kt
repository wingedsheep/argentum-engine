package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A delayed trigger's GainControl / TapUntap / GrantKeyword act on the object its creating spell
 * targeted. `CreateDelayedTriggerExecutor` bakes `ContextTarget(n)` into a concrete id when it
 * schedules the trigger (the creating spell's targets are gone when it fires); these three used to
 * fall through un-baked, so "gain control of that creature. Untap it. It gains haste" did nothing.
 */
class DelayedTriggerThreatenTargetBakingTest : FunSpec({

    val lateThreaten = card("Late Threaten") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Choose target creature. At the beginning of the next combat this turn, gain " +
            "control of that creature until end of turn. Untap it. It gains haste until end of turn."
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.CreateDelayedTrigger(
                step = Step.BEGIN_COMBAT,
                timing = DelayedTriggerTiming.THIS_TURN_ONLY,
                effect = Effects.GainControl(creature, Duration.EndOfTurn) then
                    Effects.Untap(creature) then
                    Effects.GrantKeyword(Keyword.HASTE, creature),
            )
        }
    }

    val lateTap = card("Late Tap") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Choose target creature. At the beginning of the next combat this turn, tap it."
        spell {
            target(TargetFilter.Creature)
            effect = Effects.CreateDelayedTrigger(
                step = Step.BEGIN_COMBAT,
                timing = DelayedTriggerTiming.THIS_TURN_ONLY,
                effect = Effects.Tap(EffectTarget.ContextTarget(0)),
            )
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(lateThreaten, lateTap))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.fireAtCombat() {
        passPriorityUntil(Step.BEGIN_COMBAT)
        var guard = 0
        while (stackSize > 0 && guard++ < 10) bothPass()
    }

    test("delayed GainControl, Untap and GrantKeyword act on the creating spell's target") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.tapPermanent(bears)
        d.castSpell(me, d.putCardInHand(me, "Late Threaten"), targets = listOf(bears)).error shouldBe null
        d.bothPass()
        d.state.projectedState.getController(bears) shouldBe opp

        d.fireAtCombat()

        d.state.projectedState.getController(bears) shouldBe me
        d.isTapped(bears) shouldBe false
        d.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
    }

    test("a delayed Tap acts on the creating spell's target") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.castSpell(me, d.putCardInHand(me, "Late Tap"), targets = listOf(bears)).error shouldBe null
        d.bothPass()
        d.isTapped(bears) shouldBe false

        d.fireAtCombat()

        d.isTapped(bears) shouldBe true
    }
})
