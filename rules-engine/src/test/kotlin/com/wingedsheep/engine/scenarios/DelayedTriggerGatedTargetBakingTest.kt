package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A delayed trigger whose effect sits behind a gate still acts on the object its creating spell
 * targeted.
 *
 * `CreateDelayedTriggerExecutor` bakes context-bound references (`ContextTarget(n)`) into concrete
 * ids when it schedules the trigger, because the creating spell's targets are gone by the time the
 * trigger fires (CR 603.7c). It used to look inside a [com.wingedsheep.sdk.scripting.effects.GatedEffect]
 * only for a bare "you may" — so a delayed `If(condition, …)` (or its `otherwise`, or any other gate
 * kind) kept an unresolvable `ContextTarget(0)` and silently did nothing when it fired.
 */
class DelayedTriggerGatedTargetBakingTest : FunSpec({

    // "Choose target creature. At the beginning of the next end step, if your life total is 1 or
    // more, return that creature to its owner's hand." — the gate passes, so `then` runs.
    val lateBounce = card("Late Bounce") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Choose target creature. At the beginning of the next end step, if your life " +
            "total is 1 or more, return that creature to its owner's hand."
        spell {
            target(TargetFilter.Creature)
            effect = Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.If(
                    Conditions.LifeAtLeast(1),
                    Effects.ReturnToHand(EffectTarget.ContextTarget(0))
                )
            )
        }
    }

    // Same, but the gate fails and the target is named only in `otherwise`.
    val lateBounceOtherwise = card("Late Bounce Otherwise") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Choose target creature. At the beginning of the next end step, if your life " +
            "total is 1000 or more, you gain 1 life. Otherwise, return that creature to its owner's hand."
        spell {
            target(TargetFilter.Creature)
            effect = Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.If(
                    Conditions.LifeAtLeast(1000),
                    Effects.GainLife(1),
                    otherwise = Effects.ReturnToHand(EffectTarget.ContextTarget(0))
                )
            )
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(lateBounce, lateBounceOtherwise))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun castAndFire(spellName: String) {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val spell = d.putCardInHand(me, spellName)
        d.castSpell(me, spell, targets = listOf(bears)).error shouldBe null
        d.bothPass()
        d.findPermanent(opp, "Grizzly Bears") shouldBe bears

        d.passPriorityUntil(Step.END)
        var guard = 0
        while (d.stackSize > 0 && guard++ < 10) d.bothPass()

        d.findPermanent(opp, "Grizzly Bears") shouldBe null
        d.getHand(opp) shouldContain bears
    }

    test("a delayed If(…) whose `then` names the creating spell's target acts on it") {
        castAndFire("Late Bounce")
    }

    test("a delayed If(…) whose `otherwise` names the creating spell's target acts on it") {
        castAndFire("Late Bounce Otherwise")
    }
})
