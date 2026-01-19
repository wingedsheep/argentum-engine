package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.AddCountersEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.MustBeBlockedEffect
import com.wingedsheep.rulesengine.ability.TapUntapEffect
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.CounterType
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.components.CountersComponent
import com.wingedsheep.rulesengine.ecs.components.MustBeBlockedComponent
import com.wingedsheep.rulesengine.ecs.components.TappedComponent
import com.wingedsheep.rulesengine.ecs.event.ChosenTarget
import com.wingedsheep.rulesengine.ecs.layers.Layer
import com.wingedsheep.rulesengine.ecs.layers.Modification
import com.wingedsheep.rulesengine.ecs.script.EffectEvent
import com.wingedsheep.rulesengine.ecs.script.EffectExecutor
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PermanentHandlersTest : FunSpec({

    val executor = EffectExecutor()
    val player1 = EntityId.of("p1")
    val creatureDef = CardDefinition.creature("Bear", ManaCost.parse("{G}"), setOf(Subtype.BEAR), 2, 2)

    fun setup(): Pair<GameState, EntityId> {
        var state = GameState.newGame(listOf(player1 to "Alice", EntityId.of("p2") to "Bob"))
        val (creatureId, s2) = state.createEntity(
            EntityId.generate(),
            CardComponent(creatureDef, player1),
            ControllerComponent(player1)
        )
        state = s2.addToZone(creatureId, ZoneId.BATTLEFIELD)
        return state to creatureId
    }

    context("TapUntapHandler") {
        test("Taps an untapped creature") {
            val (state, creatureId) = setup()
            val effect = TapUntapEffect(EffectTarget.TargetCreature, tap = true)
            val context = ExecutionContext(player1, player1, targets = listOf(ChosenTarget.Permanent(creatureId)))

            val result = executor.execute(state, effect, context)

            result.state.hasComponent<TappedComponent>(creatureId) shouldBe true
            result.events.first().shouldBeInstanceOf<EffectEvent.PermanentTapped>()
        }

        test("Untaps a tapped creature") {
            val (initialState, creatureId) = setup()
            val tappedState = initialState.updateEntity(creatureId) { it.with(TappedComponent) }
            
            val effect = TapUntapEffect(EffectTarget.TargetCreature, tap = false)
            val context = ExecutionContext(player1, player1, targets = listOf(ChosenTarget.Permanent(creatureId)))

            val result = executor.execute(tappedState, effect, context)

            result.state.hasComponent<TappedComponent>(creatureId) shouldBe false
            result.events.first().shouldBeInstanceOf<EffectEvent.PermanentUntapped>()
        }

        test("Handles EffectTarget.Self") {
            val (state, creatureId) = setup()
            val effect = TapUntapEffect(EffectTarget.Self, tap = true)
            // Source ID is the creature itself
            val context = ExecutionContext(player1, sourceId = creatureId)

            val result = executor.execute(state, effect, context)

            result.state.hasComponent<TappedComponent>(creatureId) shouldBe true
        }
    }

    context("ModifyStatsHandler") {
        test("Creates temporary modifier for P/T boost") {
            val (state, creatureId) = setup()
            val effect = ModifyStatsEffect(3, 3, EffectTarget.TargetCreature, untilEndOfTurn = true)
            val context = ExecutionContext(player1, player1, targets = listOf(ChosenTarget.Permanent(creatureId)))

            val result = executor.execute(state, effect, context)

            result.temporaryModifiers shouldHaveSize 1
            val mod = result.temporaryModifiers.first()
            mod.layer shouldBe Layer.PT_MODIFY
            
            val modification = mod.modification
            modification.shouldBeInstanceOf<Modification.ModifyPT>()
            modification.powerDelta shouldBe 3
            modification.toughnessDelta shouldBe 3

            result.events.first().shouldBeInstanceOf<EffectEvent.StatsModified>()
        }

        test("Handles negative values") {
            val (state, creatureId) = setup()
            val effect = ModifyStatsEffect(-2, -1, EffectTarget.TargetCreature)
            val context = ExecutionContext(player1, player1, targets = listOf(ChosenTarget.Permanent(creatureId)))

            val result = executor.execute(state, effect, context)
            val mod = result.temporaryModifiers.first().modification as Modification.ModifyPT
            mod.powerDelta shouldBe -2
            mod.toughnessDelta shouldBe -1
        }
    }

    context("AddCountersHandler") {
        test("Adds +1/+1 counters") {
            val (state, creatureId) = setup()
            val effect = AddCountersEffect("+1/+1", 2, EffectTarget.TargetCreature)
            val context = ExecutionContext(player1, player1, targets = listOf(ChosenTarget.Permanent(creatureId)))

            val result = executor.execute(state, effect, context)

            val counters = result.state.getComponent<CountersComponent>(creatureId)
            counters.shouldNotBeNull()
            counters.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2

            val event = result.events.first() as EffectEvent.CountersAdded
            event.counterType shouldBe "PLUS_ONE_PLUS_ONE"
            event.count shouldBe 2
        }

        test("Adds -1/-1 counters") {
            val (state, creatureId) = setup()
            val effect = AddCountersEffect("-1/-1", 1, EffectTarget.TargetCreature)
            val context = ExecutionContext(player1, player1, targets = listOf(ChosenTarget.Permanent(creatureId)))

            val result = executor.execute(state, effect, context)

            val counters = result.state.getComponent<CountersComponent>(creatureId)
            counters!!.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
        }

        test("Accumulates existing counters") {
            val (initialState, creatureId) = setup()
            // Add initial counters
            val stateWithCounters = initialState.updateEntity(creatureId) { 
                it.with(CountersComponent().add(CounterType.PLUS_ONE_PLUS_ONE, 1)) 
            }

            val effect = AddCountersEffect("+1/+1", 2, EffectTarget.TargetCreature)
            val context = ExecutionContext(player1, player1, targets = listOf(ChosenTarget.Permanent(creatureId)))

            val result = executor.execute(stateWithCounters, effect, context)

            val counters = result.state.getComponent<CountersComponent>(creatureId)
            counters!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3 // 1 existing + 2 new
        }
    }

    context("MustBeBlockedHandler") {
        test("Adds MustBeBlockedComponent") {
            val (state, creatureId) = setup()
            val effect = MustBeBlockedEffect(EffectTarget.TargetCreature)
            val context = ExecutionContext(player1, player1, targets = listOf(ChosenTarget.Permanent(creatureId)))

            val result = executor.execute(state, effect, context)

            result.state.hasComponent<MustBeBlockedComponent>(creatureId) shouldBe true
        }
    }

    context("GrantKeywordHandler") {
        test("Creates keyword modifier") {
            val (state, creatureId) = setup()
            val effect = GrantKeywordUntilEndOfTurnEffect(Keyword.FLYING, EffectTarget.TargetCreature)
            val context = ExecutionContext(player1, player1, targets = listOf(ChosenTarget.Permanent(creatureId)))

            val result = executor.execute(state, effect, context)

            result.temporaryModifiers shouldHaveSize 1
            val mod = result.temporaryModifiers.first()
            mod.layer shouldBe Layer.ABILITY
            
            val modification = mod.modification
            modification.shouldBeInstanceOf<Modification.AddKeyword>()
            modification.keyword shouldBe Keyword.FLYING

            val event = result.events.first() as EffectEvent.KeywordGranted
            event.keyword shouldBe Keyword.FLYING
        }
    }
})