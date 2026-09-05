package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.StatsModifiedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.permanent.stats.SwitchPowerToughnessExecutor
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.SwitchPowerToughnessEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SwitchPowerToughnessScenarioTest : FunSpec({
    val wall = card("Asymmetric Switch Test") {
        manaCost = "{U}"
        typeLine = "Creature — Wall"
        power = 1
        toughness = 5
    }
    val switch = card("Permanent Switch Test") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val permanent = target("permanent", Targets.Permanent)
            effect = Effects.SwitchPowerToughness(permanent, Duration.Permanent)
        }
    }
    val animate = card("Animate Switch Test") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val permanent = target("permanent", Targets.Permanent)
            effect = Effects.BecomeCreature(target = permanent, power = 2, toughness = 5)
        }
    }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(switch, animate, wall))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("switch can wait on a noncreature then apply to its animated stats") {
        val d = driver()
        val land = d.putLandOnBattlefield(d.player1, "Forest")
        val spell = d.putCardInHand(d.player1, switch.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, spell, listOf(land)).error shouldBe null
        d.bothPass().error shouldBe null
        d.state.projectedState.getPower(land) shouldBe null
        d.state.projectedState.getToughness(land) shouldBe null
        val animation = d.putCardInHand(d.player1, animate.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, animation, listOf(land)).error shouldBe null
        d.bothPass().error shouldBe null
        d.state.projectedState.getPower(land) shouldBe 5
        d.state.projectedState.getToughness(land) shouldBe 2
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.getPower(land) shouldBe null
        d.state.floatingEffects.any { it.duration == Duration.Permanent } shouldBe true
    }

    test("a permanent switch targeting another creature survives its spell leaving and cleanup") {
        val d = driver()
        val creature = d.putCreatureOnBattlefield(d.player2, wall.name)
        val spell = d.putCardInHand(d.player1, switch.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, spell, listOf(creature)).error shouldBe null
        d.bothPass().error shouldBe null
        d.state.projectedState.getPower(creature) shouldBe 5
        d.state.projectedState.getToughness(creature) shouldBe 1
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.getPower(creature) shouldBe 5
        d.state.projectedState.getToughness(creature) shouldBe 1
    }

    test("executor emits actual stat changes and leaves its input immutable") {
        val d = driver()
        val creature = d.putCreatureOnBattlefield(d.player1, wall.name)
        val before = d.state
        val result = SwitchPowerToughnessExecutor().execute(before,
            SwitchPowerToughnessEffect(EffectTarget.Self),
            EffectContext(sourceId = creature, controllerId = d.player1))
        before.projectedState.getPower(creature) shouldBe 1
        result.state.projectedState.getPower(creature) shouldBe 5
        val event = result.events.filterIsInstance<StatsModifiedEvent>().single()
        event.powerChange shouldBe 4
        event.toughnessChange shouldBe -4
    }
})
