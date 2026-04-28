package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter

/**
 * Applies "enters with counters" replacement effects sourced from *other* battlefield
 * permanents (e.g., Gev, Scaled Scorch granting +1/+1 counters to creatures you control
 * that enter the battlefield).
 *
 * Used both by [com.wingedsheep.engine.mechanics.stack.StackResolver] when a spell
 * resolves onto the battlefield, and by token-creation executors when tokens enter
 * (tokens have no cast step but Rule 614 replacement effects still apply to them).
 */
object EntersWithCountersHelper {

    private val dynamicAmountEvaluator = DynamicAmountEvaluator()
    private val predicateEvaluator = PredicateEvaluator()

    /**
     * Apply both the entering entity's own [EntersWithCounters] / [EntersWithDynamicCounters]
     * replacement effects (from its [CardDefinition]) and any global ones sourced from other
     * battlefield permanents. Used by callers that put a permanent on the battlefield from a
     * non-stack zone (e.g., [com.wingedsheep.engine.handlers.effects.zones.MoveToZoneEffectExecutor]
     * when reanimating from graveyard).
     *
     * Stack resolution has its own pre-battlefield application path
     * ([com.wingedsheep.engine.mechanics.stack.StackResolver.applyEntersWithCounters]) and should
     * not call this method; it would double-apply.
     */
    fun applyEntersWithCounters(
        state: GameState,
        enteringEntityId: EntityId,
        enteringControllerId: EntityId,
        cardRegistry: CardRegistry,
        xValue: Int? = null
    ): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()
        val container = newState.getEntity(enteringEntityId) ?: return newState to events
        val cardComponent = container.get<CardComponent>() ?: return newState to events
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return newState to events
        val entityName = cardComponent.name

        for (effect in cardDef.script.replacementEffects) {
            when (effect) {
                is EntersWithCounters -> {
                    val counterType = resolveCounterType(effect.counterType)
                    val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                        newState, enteringEntityId, counterType, effect.count, placerId = enteringControllerId
                    )
                    val current = newState.getEntity(enteringEntityId)?.get<CountersComponent>() ?: CountersComponent()
                    newState = newState.updateEntity(enteringEntityId) { c ->
                        c.with(current.withAdded(counterType, modifiedCount))
                    }
                    events.add(CountersAddedEvent(enteringEntityId, effect.counterType.description, modifiedCount, entityName))
                }
                is EntersWithDynamicCounters -> {
                    if (effect.otherOnly) continue
                    val counterType = resolveCounterType(effect.counterType)
                    val context = EffectContext(
                        sourceId = enteringEntityId,
                        controllerId = enteringControllerId,
                        opponentId = newState.turnOrder.firstOrNull { it != enteringControllerId },
                        xValue = xValue
                    )
                    val count = dynamicAmountEvaluator.evaluate(newState, effect.count, context)
                    if (count > 0) {
                        val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                            newState, enteringEntityId, counterType, count, placerId = enteringControllerId
                        )
                        val current = newState.getEntity(enteringEntityId)?.get<CountersComponent>() ?: CountersComponent()
                        newState = newState.updateEntity(enteringEntityId) { c ->
                            c.with(current.withAdded(counterType, modifiedCount))
                        }
                        events.add(CountersAddedEvent(enteringEntityId, effect.counterType.description, modifiedCount, entityName))
                    }
                }
                else -> { /* Other replacement effects handled elsewhere */ }
            }
        }

        val (globalState, globalEvents) = applyGlobalEntersWithCounters(
            newState, enteringEntityId, enteringControllerId
        )
        newState = globalState
        events.addAll(globalEvents)

        return newState to events
    }

    /**
     * Scan battlefield permanents for [EntersWithCounters] / [EntersWithDynamicCounters]
     * replacement effects that apply to the entering entity, and add the counters.
     *
     * @param state The game state after the entering entity has been added to the battlefield.
     * @param enteringEntityId The entity that just entered the battlefield.
     * @param enteringControllerId The controller of the entering entity.
     */
    fun applyGlobalEntersWithCounters(
        state: GameState,
        enteringEntityId: EntityId,
        enteringControllerId: EntityId,
    ): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()
        val entityName = newState.getEntity(enteringEntityId)?.get<CardComponent>()?.name ?: ""

        for (sourceId in newState.getBattlefield()) {
            if (sourceId == enteringEntityId) continue
            val container = newState.getEntity(sourceId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = container.get<ControllerComponent>()?.playerId ?: continue

            for (effect in replacementComponent.replacementEffects) {
                when (effect) {
                    is EntersWithCounters -> {
                        if (effect.selfOnly) continue
                        if (!matchesEnterFilter(effect.appliesTo, enteringEntityId, sourceControllerId, newState)) continue
                        val counterType = resolveCounterType(effect.counterType)
                        val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                            newState, enteringEntityId, counterType, effect.count, placerId = enteringControllerId
                        )
                        val current = newState.getEntity(enteringEntityId)?.get<CountersComponent>() ?: CountersComponent()
                        newState = newState.updateEntity(enteringEntityId) { c ->
                            c.with(current.withAdded(counterType, modifiedCount))
                        }
                        events.add(CountersAddedEvent(enteringEntityId, effect.counterType.description, modifiedCount, entityName))
                    }
                    is EntersWithDynamicCounters -> {
                        if (!effect.otherOnly) continue
                        if (!matchesEnterFilter(effect.appliesTo, enteringEntityId, sourceControllerId, newState)) continue
                        val counterType = resolveCounterType(effect.counterType)
                        val context = EffectContext(
                            sourceId = sourceId,
                            controllerId = sourceControllerId,
                            opponentId = newState.turnOrder.firstOrNull { it != sourceControllerId }
                        )
                        val count = dynamicAmountEvaluator.evaluate(newState, effect.count, context)
                        if (count > 0) {
                            val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                                newState, enteringEntityId, counterType, count, placerId = enteringControllerId
                            )
                            val current = newState.getEntity(enteringEntityId)?.get<CountersComponent>() ?: CountersComponent()
                            newState = newState.updateEntity(enteringEntityId) { c ->
                                c.with(current.withAdded(counterType, modifiedCount))
                            }
                            events.add(CountersAddedEvent(enteringEntityId, effect.counterType.description, modifiedCount, entityName))
                        }
                    }
                    else -> { /* Other replacement effects handled elsewhere */ }
                }
            }
        }
        return newState to events
    }

    private fun matchesEnterFilter(
        event: com.wingedsheep.sdk.scripting.GameEvent,
        enteringEntityId: EntityId,
        sourceControllerId: EntityId,
        state: GameState,
    ): Boolean {
        if (event !is com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent) return false
        if (event.to != Zone.BATTLEFIELD) return false
        val filter = event.filter

        val predicateContext = PredicateContext(
            sourceId = enteringEntityId,
            controllerId = sourceControllerId
        )
        return predicateEvaluator.matches(state, enteringEntityId, filter, predicateContext)
    }

    fun resolveCounterType(filter: CounterTypeFilter): CounterType {
        return when (filter) {
            is CounterTypeFilter.Any -> CounterType.PLUS_ONE_PLUS_ONE
            is CounterTypeFilter.PlusOnePlusOne -> CounterType.PLUS_ONE_PLUS_ONE
            is CounterTypeFilter.MinusOneMinusOne -> CounterType.MINUS_ONE_MINUS_ONE
            is CounterTypeFilter.Loyalty -> CounterType.LOYALTY
            is CounterTypeFilter.Named -> {
                try {
                    CounterType.valueOf(filter.name.uppercase().replace(' ', '_'))
                } catch (_: IllegalArgumentException) {
                    CounterType.PLUS_ONE_PLUS_ONE
                }
            }
        }
    }
}
