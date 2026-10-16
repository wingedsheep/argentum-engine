package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.GainLifeInsteadOfLosingLife
import com.wingedsheep.sdk.scripting.ModifyLifeGain
import com.wingedsheep.sdk.scripting.ModifyLifeLoss
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.effects.LifeLossOrigin
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe

/** GainLifeInsteadOfLosingLife: origin-scoped conversion of non-damage life loss into life gain. */
class GainLifeInsteadOfLosingLifeTest : ScenarioTestBase() {
    private fun permanent(name: String, block: CardBuilder.() -> Unit) = card(name) {
        manaCost = "{0}"; typeLine = "Artifact"; block()
    }
    private val radConverter = permanent("Rad Converter Probe") {
        replacementEffect(GainLifeInsteadOfLosingLife(origin = LifeLossOrigin.RADIATION))
    }
    private val effectConverter = permanent("Effect Converter Probe") {
        replacementEffect(GainLifeInsteadOfLosingLife(origin = LifeLossOrigin.EFFECT))
    }
    private val doubleLoss = permanent("Double Loss Probe") {
        replacementEffect(ModifyLifeLoss(multiplier = 2, appliesTo = EventPattern.LifeLossEvent(Player.Each)))
    }
    private val lessLoss = permanent("Less Loss Probe") {
        replacementEffect(ModifyLifeLoss(modifier = -1, appliesTo = EventPattern.LifeLossEvent(Player.Each)))
    }
    private val doubleGain = permanent("Double Gain Probe") {
        replacementEffect(ModifyLifeGain(multiplier = 2, appliesTo = EventPattern.LifeGainEvent(Player.Each)))
    }
    private val noGain = permanent("No Gain Probe") {
        replacementEffect(PreventLifeGain(appliesTo = EventPattern.LifeGainEvent(Player.Each)))
    }
    private val gainWatcher = permanent("Gain Watcher Probe") {
        triggeredAbility {
            trigger = Triggers.you.gainsLife()
            effect = Effects.DrawCards(1)
        }
    }
    private val loseTwo = card("Lose Two Probe") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.LoseLife(2, EffectTarget.Controller) }
    }
    private val drain = card("Drain Probe") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.DrainLife(2) }
    }

    private fun TestGame.radCount(): Int =
        state.getEntity(player1Id)?.get<CountersComponent>()?.getCount(CounterType.RAD) ?: 0

    /** Player 1 has [rads] rad counters and a library of bears; the radiation trigger is on the stack. */
    private fun radiation(rads: Int, vararg onBattlefield: String): TestGame {
        val b = scenario().withPlayers().withCardInLibrary(2, "Forest")
        onBattlefield.forEach { b.withCardOnBattlefield(1, it) }
        repeat(rads + 2) { b.withCardInLibrary(1, "Grizzly Bears") }
        val g = b.build()
        g.state = g.state.copy(phase = Phase.BEGINNING, step = Step.DRAW)
        g.state = g.state.updateEntity(g.player1Id) { it.with(CountersComponent(mapOf(CounterType.RAD to rads))) }
        g.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        return g
    }

    init {
        listOf(radConverter, effectConverter, doubleLoss, lessLoss, doubleGain, noGain, gainWatcher, loseTwo, drain)
            .forEach { cardRegistry.register(it) }

        test("radiation life loss becomes one life-gain event; rad counters still come off") {
            val g = radiation(3, radConverter.name)
            val events = g.resolveStack().flatMap { it.events }.filterIsInstance<LifeChangedEvent>()
            g.getLifeTotal(1) shouldBe 23
            g.radCount() shouldBe 0
            events.single().reason shouldBe LifeChangeReason.LIFE_GAIN
        }

        test("a radiation-scoped conversion leaves other life loss alone") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, radConverter.name)
                .withCardInHand(1, loseTwo.name).withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            g.castSpell(1, loseTwo.name).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 18
        }

        test("it only applies to its controller's radiation") {
            val g = scenario().withPlayers().withCardOnBattlefield(2, radConverter.name).withCardInLibrary(2, "Forest")
                .also { b -> repeat(4) { b.withCardInLibrary(1, "Grizzly Bears") } }.build()
            g.state = g.state.copy(phase = Phase.BEGINNING, step = Step.DRAW)
            g.state = g.state.updateEntity(g.player1Id) { it.with(CountersComponent(mapOf(CounterType.RAD to 2))) }
            g.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 18
        }

        test("an increase to the loss is applied before converting, so the gain grows") {
            val g = radiation(2, radConverter.name, doubleLoss.name)
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 24
        }

        test("a reduction to the loss is skipped by converting first") {
            val g = radiation(2, radConverter.name, lessLoss.name)
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 22
        }

        test("with both an increase and a reduction, the increase applies and the reduction is skipped") {
            val g = radiation(2, radConverter.name, doubleLoss.name, lessLoss.name)
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 24
        }

        test("the gain is ordinary life gain: modifiers and gain triggers see it") {
            val g = radiation(2, radConverter.name, doubleGain.name, gainWatcher.name)
            val hand = g.handSize(1)
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 24
            g.handSize(1) shouldBe hand + 1
        }

        test("when life can't be gained, the player neither gains nor loses") {
            val g = radiation(2, radConverter.name, noGain.name)
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 20
            g.radCount() shouldBe 0
        }

        test("a drain whose victim converts the loss gives the drainer nothing") {
            val g = scenario().withPlayers().withCardOnBattlefield(2, effectConverter.name)
                .withCardInHand(1, drain.name).withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            g.castSpell(1, drain.name).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(2) shouldBe 22
            g.getLifeTotal(1) shouldBe 20
        }
    }
}
