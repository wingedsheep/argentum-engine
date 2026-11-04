package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.effects.damage.DamageReplacementChain
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SimultaneousLoopDamageTest : FunSpec({
    val store = card("Loop Counter Store") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        replacementEffect(ReplaceDamageWithCounters(CounterType.PLUS_ONE_PLUS_ONE,
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.You)))
        replacementEffect(ModifyDamageAmount(dynamicModifier = DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE),
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.Opponent)))
    }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + store)
        initMirrorMatch(Deck.of("Mountain" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    for (space in listOf(IterationSpace.Targets, IterationSpace.TargetsOf("victims"), IterationSpace.Players(Player.Each))) {
        test("atomic $space defers replacement results until all damage is replaced") {
            val d = driver()
            val host = d.putPermanentOnBattlefield(d.player1, store.name)
            val players = listOf(ChosenTarget.Player(d.player1), ChosenTarget.Player(d.player2))
            val context = EffectContext(host, d.player1, targets = players,
                pipeline = PipelineState(namedTargets = mapOf("victims[0]" to players[0], "victims[1]" to players[1])))
            val target = when (space) {
                IterationSpace.Targets -> EffectTarget.ContextTarget(0)
                is IterationSpace.TargetsOf -> EffectTarget.IterationEntity
                else -> EffectTarget.Controller
            }
            val result = d.services.effectExecutorRegistry.execute(d.state,
                ForEachEffect(space, Effects.DealDamage(3, target)), context)
            result.error shouldBe null
            result.state.pendingDecision shouldBe null
            d.replaceState(result.state)
            d.getLifeTotal(d.player1) shouldBe 20
            d.getLifeTotal(d.player2) shouldBe 17
            d.state.getEntity(host)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
        }
    }

    for (space in listOf(IterationSpace.Group(GroupFilter.AllCreatures), IterationSpace.Collection("sources"))) {
        test("atomic $space retains each dynamic amount and damage source") {
            val d = driver()
            val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            val giant = d.putCreatureOnBattlefield(d.player1, "Hill Giant")
            val context = EffectContext(bear, d.player1,
                pipeline = PipelineState(storedCollections = mapOf("sources" to listOf(bear, giant))))
            val result = d.services.effectExecutorRegistry.execute(d.state, ForEachEffect(space,
                Effects.DealDamage(DynamicAmounts.powerOf(EffectTarget.IterationEntity),
                    EffectTarget.PlayerRef(Player.EachOpponent), damageSource = EffectTarget.IterationEntity,
                    damageDealtVariable = "dealt")), context)
            result.error shouldBe null
            result.updatedStoredNumbers["dealt"] shouldBe 5
            result.events.filterIsInstance<DamageDealtEvent>().associate { it.sourceId to it.amount } shouldBe
                mapOf(bear to 2, giant to 3)
        }
    }

    for (noRegenerate in listOf(false, true)) {
        test("group damage respects noRegenerate=$noRegenerate against a regeneration shield") {
            val d = driver()
            val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
            val source = d.putPermanentOnBattlefield(d.player1, store.name)
            val context = EffectContext(source, d.player1)
            d.replaceState(d.state.addFloatingEffect(Layer.ABILITY,
                SerializableModification.RegenerationShield, setOf(bear), Duration.EndOfTurn,
                EffectContext(bear, d.player2)))
            val result = d.services.effectExecutorRegistry.execute(d.state,
                Effects.ForEachInGroup(GroupFilter.AllCreatures,
                    Effects.DealDamage(3, EffectTarget.IterationEntity), noRegenerate = noRegenerate), context)
            result.error shouldBe null
            result.state.getEntity(bear)?.get<DamageComponent>()?.amount shouldBe 3
            // Direct executor calls bypass the action processor's settlement boundary.
            d.replaceState(d.services.sbaChecker.checkAndApply(result.state).newState)
            (bear in d.getGraveyard(d.player2)) shouldBe noRegenerate
            (bear in d.getCreatures(d.player2)) shouldBe !noRegenerate
            if (!noRegenerate) {
                (d.state.getEntity(bear)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
                d.state.floatingEffects.any {
                    it.effect.modification is SerializableModification.RegenerationShield
                } shouldBe false
            }
        }
    }

    test("group no-regeneration preparation is preserved once when damage replays after a choice") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val third = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val source = d.putPermanentOnBattlefield(d.player1, store.name)
        d.replaceState(d.state.addFloatingEffect(Layer.ABILITY,
            SerializableModification.PreventNextDamage(1), setOf(bear, giant, third), Duration.EndOfTurn,
            EffectContext(giant, d.player2)))
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.ForEachInGroup(GroupFilter.AllCreatures,
                Effects.DealDamage(1, EffectTarget.IterationEntity), noRegenerate = true),
            EffectContext(source, d.player1))
        result.outcome.shouldBeInstanceOf<Outcome.Paused>()
        d.replaceState(result.state)
        val markers = d.state.floatingEffects.filter {
            it.effect.modification is SerializableModification.CantBeRegenerated
        }
        markers.size shouldBe 3
        markers.flatMap { it.effect.affectedEntities }.toSet() shouldBe setOf(bear, giant, third)
        val choice = d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        d.submitDecision(d.player2, NumberChosenResponse(choice.id, 0)).error shouldBe null
        d.state.floatingEffects.filter {
            it.effect.modification is SerializableModification.CantBeRegenerated
        } shouldBe markers
        val nextChoice = d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        d.submitDecision(d.player2, NumberChosenResponse(nextChoice.id, 0)).error shouldBe null
        d.pendingDecision shouldBe null
        d.state.floatingEffects.filter {
            it.effect.modification is SerializableModification.CantBeRegenerated
        } shouldBe markers
        d.state.getEntity(bear)?.get<DamageComponent>()?.amount shouldBe 1
        d.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 1
        (d.state.getEntity(third)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
    }

    test("shared shield allocation replays the outer loop and publishes total damage after a pause") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val source = d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        val context = EffectContext(source, d.player1,
            targets = listOf(ChosenTarget.Permanent(bear), ChosenTarget.Permanent(giant)))
        d.replaceState(d.state.addFloatingEffect(Layer.ABILITY,
            SerializableModification.PreventNextDamage(1), setOf(bear, giant), Duration.EndOfTurn,
            EffectContext(giant, d.player2)))
        val effect = CompositeEffect(listOf(
            ForEachTargetEffect(listOf(Effects.DealDamage(1, EffectTarget.ContextTarget(0), damageDealtVariable = "dealt"))),
            Effects.GainLife(DynamicAmount.VariableReference("dealt"))))
        val result = d.services.effectExecutorRegistry.execute(d.state, effect, context)
        result.outcome.shouldBeInstanceOf<Outcome.Paused>()
        d.replaceState(result.state)
        (d.state.getEntity(bear)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        val choice = d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        choice.playerId shouldBe d.player2
        choice.minValue shouldBe 0
        choice.maxValue shouldBe 1
        d.submitDecision(d.player2, NumberChosenResponse(choice.id, 0)).error shouldBe null
        d.pendingDecision shouldBe null
        d.getLifeTotal(d.player1) shouldBe 21
        d.state.getEntity(bear)?.get<DamageComponent>()?.amount shouldBe 1
        (d.state.getEntity(giant)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
    }

    test("a composite loop body keeps its per-item sequence") {
        val d = driver()
        val host = d.putPermanentOnBattlefield(d.player1, store.name)
        val context = EffectContext(host, d.player1,
            targets = listOf(ChosenTarget.Player(d.player1), ChosenTarget.Player(d.player2)))
        val effect = ForEachTargetEffect(listOf(
            Effects.DealDamage(3, EffectTarget.ContextTarget(0)), Effects.GainLife(0)))
        val result = d.services.effectExecutorRegistry.execute(d.state, effect, context)
        d.replaceState(result.state)
        d.getLifeTotal(d.player2) shouldBe 14
    }

    test("next source damage prevention covers every recipient and is consumed once") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val context = EffectContext(source, d.player1,
            targets = listOf(ChosenTarget.Permanent(bear), ChosenTarget.Permanent(giant)))
        d.replaceState(d.state.addFloatingEffect(Layer.ABILITY,
            SerializableModification.PreventNextDamageFromSourceShield(source, "next-damage"),
            emptySet(), Duration.EndOfTurn, EffectContext(giant, d.player2)))
        val effect = ForEachTargetEffect(listOf(Effects.DealDamage(1, EffectTarget.ContextTarget(0))))
        val result = d.services.effectExecutorRegistry.execute(d.state, effect, context)
        result.state.pendingDecision shouldBe null
        result.events.filterIsInstance<DamageDealtEvent>() shouldBe emptyList()
        result.events.filterIsInstance<DamagePreventedEvent>().sumOf { it.amount } shouldBe 2
        result.state.floatingEffects shouldBe emptyList()
        val next = d.services.effectExecutorRegistry.execute(result.state, effect, context)
        next.events.filterIsInstance<DamageDealtEvent>().sumOf { it.amount } shouldBe 2
    }

    test("whole source shield handles unpreventable recipients independently") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        val state = d.state.addFloatingEffect(Layer.ABILITY,
            SerializableModification.PreventNextDamageFromSourceShield(source, "next-damage"),
            emptySet(), Duration.EndOfTurn, EffectContext(source, d.player1))
        val result = DamageReplacementChain.deal(d.zones, state, listOf(
            DamageReplacementChain.Instance(source, d.player1, 1, cantBePrevented = true),
            DamageReplacementChain.Instance(source, d.player2, 1)))
        result.events.filterIsInstance<DamageDealtEvent>().map { it.targetId } shouldBe listOf(d.player1)
        result.events.filterIsInstance<DamagePreventedEvent>().sumOf { it.amount } shouldBe 1
    }
})
