package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.KeywordGrantedEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
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
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.EntersWithKeywords
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter

/**
 * Applies "enters with …" replacement effects (CR 614.1c) — counters
 * ([EntersWithCounters] / [EntersWithDynamicCounters]) and keywords ([EntersWithKeywords]) —
 * both the entering permanent's own effects and global ones sourced from *other* battlefield
 * permanents (e.g., Gev, Scaled Scorch granting +1/+1 counters to creatures you control that
 * enter the battlefield).
 *
 * Used by [com.wingedsheep.engine.mechanics.stack.StackResolver] when a spell resolves onto the
 * battlefield, by token-creation executors when tokens enter (tokens have no cast step but
 * Rule 614 replacement effects still apply to them), and by non-stack entry paths such as
 * [com.wingedsheep.engine.handlers.effects.zones.MoveToZoneEffectExecutor] (reanimation).
 */
object EntersWithReplacements {

    private val dynamicAmountEvaluator = DynamicAmountEvaluator()
    private val predicateEvaluator = PredicateEvaluator()
    private val conditionEvaluator = com.wingedsheep.engine.handlers.ConditionEvaluator()

    /**
     * Apply a **granted-Riot** chosen branch to [entityId] (controlled by [controllerId]): the
     * `counter` mode adds one +1/+1 counter (honoring counter-placement replacements); the `haste`
     * mode grants a [Duration.Permanent] floating HASTE.
     *
     * This is a **deliberate parallel apply path**, not a duplicate: the printed `riot()` DSL applies
     * its branch via a mode-gated `EntersWithCounters` and a mode-gated `ConditionalStaticAbility`
     * that both live on the *printed card* and are read from its [CardDefinition]. A permanent that
     * merely has RIOT *granted* ("Other Spiders you control have riot") has none of those statics on
     * its own definition, so the printed path ([applyFromDefinition] / the layer system) has nothing
     * to fire. The chosen branch is therefore assembled here from the same primitives the printed
     * path uses ([ReplacementEffectUtils.applyCounterPlacementModifiers] for the counter,
     * `addFloatingEffect(GrantKeyword(HASTE))` for haste), so the two paths stay behaviorally aligned.
     */
    fun applyGrantedRiotBranch(
        state: GameState,
        entityId: EntityId,
        controllerId: EntityId,
        modeId: String,
        entityName: String,
    ): Pair<GameState, List<GameEvent>> {
        val context = EffectContext(sourceId = entityId, controllerId = controllerId)
        val events = mutableListOf<GameEvent>()
        return when (modeId) {
            com.wingedsheep.sdk.dsl.RIOT_MODE_COUNTER -> {
                val counterType = com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE
                val count = ReplacementEffectUtils.applyCounterPlacementModifiers(
                    state, entityId, counterType, 1, placerId = controllerId
                )
                if (count <= 0) return state to events
                val current = state.getEntity(entityId)?.get<CountersComponent>() ?: CountersComponent()
                var newState = state.updateEntity(entityId) { c -> c.with(current.withAdded(counterType, count)) }
                val (afterMark, firstThisTurn) = DamageUtils.recordCounterPlacement(newState, entityId)
                newState = afterMark
                events.add(CountersAddedEvent(entityId, "+1/+1", count, entityName, firstThisTurn, placedBy = controllerId))
                newState to events
            }
            com.wingedsheep.sdk.dsl.RIOT_MODE_HASTE -> {
                val newState = state.addFloatingEffect(
                    layer = Layer.ABILITY,
                    modification = SerializableModification.GrantKeyword(com.wingedsheep.sdk.core.Keyword.HASTE.name),
                    affectedEntities = setOf(entityId),
                    duration = Duration.Permanent,
                    context = context,
                )
                events.add(KeywordGrantedEvent(targetId = entityId, targetName = entityName, keyword = "haste", sourceName = entityName))
                newState to events
            }
            else -> state to events
        }
    }

    /**
     * Apply both the entering entity's own enters-with replacement effects (from its
     * [CardDefinition], looked up via [cardRegistry]) and any global ones sourced from other
     * battlefield permanents. Used by callers that put a permanent on the battlefield from a
     * non-stack zone.
     *
     * Stack resolution has its own pre-battlefield application path
     * ([com.wingedsheep.engine.mechanics.stack.StackResolver.applyEntersWithReplacements],
     * which passes the resolving spell's card definition and cast context explicitly) and
     * should not call this method; it would double-apply.
     */
    fun applyOnEntry(
        state: GameState,
        enteringEntityId: EntityId,
        enteringControllerId: EntityId,
        cardRegistry: CardRegistry,
        xValue: Int? = null
    ): Pair<GameState, List<GameEvent>> {
        val container = state.getEntity(enteringEntityId) ?: return state to emptyList()
        val cardComponent = container.get<CardComponent>() ?: return state to emptyList()
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return state to emptyList()

        var newState = state
        val events = mutableListOf<GameEvent>()

        val (ownState, ownEvents) = applyFromDefinition(
            newState, enteringEntityId, cardDef, enteringControllerId, xValue
        )
        newState = ownState
        events.addAll(ownEvents)

        val (globalState, globalEvents) = applyGlobal(
            newState, enteringEntityId, enteringControllerId
        )
        newState = globalState
        events.addAll(globalEvents)

        return newState to events
    }

    /**
     * Apply the entering entity's *own* enters-with replacement effects from [cardDef].
     * Shared by [applyOnEntry] and the stack-resolution path (which has the resolving
     * spell's definition and cast context — [xValue] / [totalManaSpent] — at hand).
     */
    fun applyFromDefinition(
        state: GameState,
        entityId: EntityId,
        cardDef: CardDefinition,
        controllerId: EntityId,
        xValue: Int? = null,
        totalManaSpent: Int = 0
    ): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()
        val entityName = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: ""

        for (effect in cardDef.script.replacementEffects) {
            when (effect) {
                is EntersWithCounters -> {
                    // Skip "other only" effects when applying to self (Metallic Mimic's "each other
                    // creature you control … enters with an additional +1/+1 counter" must not
                    // counter the Mimic itself). Mirrors the EntersWithDynamicCounters path below.
                    if (effect.otherOnly) continue
                    if (effect.condition != null) {
                        val condContext = EffectContext(
                            sourceId = entityId,
                            controllerId = controllerId,
                        )
                        if (!conditionEvaluator.evaluate(newState, effect.condition!!, condContext)) continue
                    }
                    val (afterCounters, counterEvents) = placeEntryCounters(
                        newState, entityId, effect.counterType, effect.count, controllerId, entityName
                    )
                    newState = afterCounters
                    events.addAll(counterEvents)
                }
                is EntersWithDynamicCounters -> {
                    // Skip "other only" effects when applying to self (e.g., Gev)
                    if (effect.otherOnly) continue
                    val context = EffectContext(
                        sourceId = entityId,
                        controllerId = controllerId,
                        xValue = xValue,
                        totalManaSpent = totalManaSpent
                    )
                    val count = dynamicAmountEvaluator.evaluate(newState, effect.count, context)
                    val (afterCounters, counterEvents) = placeEntryCounters(
                        newState, entityId, effect.counterType, count, controllerId, entityName
                    )
                    newState = afterCounters
                    events.addAll(counterEvents)
                }
                is EntersWithKeywords -> {
                    val context = EffectContext(
                        sourceId = entityId,
                        controllerId = controllerId,
                    )
                    if (effect.condition != null &&
                        !conditionEvaluator.evaluate(newState, effect.condition!!, context)
                    ) continue
                    val (grantState, grantEvents) = grantKeywords(newState, effect, entityId, entityName, context)
                    newState = grantState
                    events.addAll(grantEvents)
                }
                else -> { /* Other replacement effects handled elsewhere */ }
            }
        }
        return newState to events
    }

    /**
     * Place [count] counters of [counterType] on [entityId] as it enters (CR 614.1c), honouring
     * counter-placement modifiers (Hardened Scales, Solemnity) and recording the placement for
     * "put a counter on a permanent this turn" trackers.
     *
     * Shared by the enters-with replacement branches above and by the copy path
     * ([com.wingedsheep.engine.handlers.continuations.ModalAndCloneContinuationResumer]), where
     * `EntersAsCopy.additionalCounters` carries the "except it enters with N additional +1/+1
     * counters on it" rider — the copy replaces the permanent's own text, so those counters can't
     * come from a self-targeted [EntersWithCounters]. A non-positive [count] is a no-op.
     */
    fun placeEntryCounters(
        state: GameState,
        entityId: EntityId,
        counterType: CounterTypeFilter,
        count: Int,
        controllerId: EntityId,
        entityName: String,
    ): Pair<GameState, List<GameEvent>> {
        if (count <= 0) return state to emptyList()
        val resolved = resolveCounterType(counterType)
        val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
            state, entityId, resolved, count, placerId = controllerId
        )
        val current = state.getEntity(entityId)?.get<CountersComponent>() ?: CountersComponent()
        var newState = state.updateEntity(entityId) { c ->
            c.with(current.withAdded(resolved, modifiedCount))
        }
        val (afterMark, firstThisTurn) = DamageUtils.recordCounterPlacement(newState, entityId)
        newState = afterMark
        return newState to listOf(
            CountersAddedEvent(
                entityId, counterType.description, modifiedCount, entityName, firstThisTurn,
                placedBy = controllerId
            )
        )
    }

    /**
     * Scan battlefield permanents for enters-with replacement effects that apply to the
     * entering entity, and apply them (counters added / keywords granted).
     *
     * @param state The game state after the entering entity has been added to the battlefield.
     * @param enteringEntityId The entity that just entered the battlefield.
     * @param enteringControllerId The controller of the entering entity.
     */
    fun applyGlobal(
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
                        if (!matchesEnterFilter(effect.appliesTo, enteringEntityId, sourceId, sourceControllerId, newState)) continue
                        if (effect.condition != null) {
                            // A non-self "enters with counters" condition describes the ENTERING
                            // creature ("it was cast from your graveyard", Leonardo), not the
                            // replacement source — evaluate it against the entering entity, mirroring
                            // the EntersWithDynamicCounters path below. controllerId stays the source's
                            // controller so "you control" resolves to the effect's controller.
                            val condContext = EffectContext(
                                sourceId = enteringEntityId,
                                controllerId = sourceControllerId,
                                affectedEntityId = enteringEntityId,
                            )
                            if (!conditionEvaluator.evaluate(newState, effect.condition!!, condContext)) continue
                        }
                        val counterType = resolveCounterType(effect.counterType)
                        val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                            newState, enteringEntityId, counterType, effect.count, placerId = enteringControllerId
                        )
                        val current = newState.getEntity(enteringEntityId)?.get<CountersComponent>() ?: CountersComponent()
                        newState = newState.updateEntity(enteringEntityId) { c ->
                            c.with(current.withAdded(counterType, modifiedCount))
                        }
                        val (afterMark, firstThisTurn) = DamageUtils.recordCounterPlacement(newState, enteringEntityId)
                        newState = afterMark
                        events.add(CountersAddedEvent(enteringEntityId, effect.counterType.description, modifiedCount, entityName, firstThisTurn, placedBy = enteringControllerId))
                    }
                    is EntersWithDynamicCounters -> {
                        if (!effect.otherOnly) continue
                        if (!matchesEnterFilter(effect.appliesTo, enteringEntityId, sourceId, sourceControllerId, newState)) continue
                        val counterType = resolveCounterType(effect.counterType)
                        // An "enters with N counters" count always describes the ENTERING object,
                        // not the replacement source — the counters go on the entering permanent and
                        // any "it" in the count refers to it (CR 121.6 / 614). Mirror the self path
                        // (StackResolver.applyEntersWithReplacements uses sourceId = the entering
                        // entity) so amounts like DistinctColorsManaSpent ("...colors of mana spent
                        // to cast IT") read the entering creature's own cast, not the source's.
                        // controllerId stays the source's controller so player-scoped counts (Gev's
                        // TurnTracking(Player.You)) keep reading the source controller's trackers.
                        val context = EffectContext(
                            sourceId = enteringEntityId,
                            controllerId = sourceControllerId,
                            affectedEntityId = enteringEntityId,
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
                            val (afterMark, firstThisTurn) = DamageUtils.recordCounterPlacement(newState, enteringEntityId)
                            newState = afterMark
                            events.add(CountersAddedEvent(enteringEntityId, effect.counterType.description, modifiedCount, entityName, firstThisTurn, placedBy = enteringControllerId))
                        }
                    }
                    is EntersWithKeywords -> {
                        if (effect.selfOnly) continue
                        if (!matchesEnterFilter(effect.appliesTo, enteringEntityId, sourceId, sourceControllerId, newState)) continue
                        // Like the counters branch above: the condition describes the ENTERING
                        // permanent; controllerId stays the source's controller.
                        if (effect.condition != null) {
                            val condContext = EffectContext(
                                sourceId = enteringEntityId,
                                controllerId = sourceControllerId,
                                affectedEntityId = enteringEntityId,
                            )
                            if (!conditionEvaluator.evaluate(newState, effect.condition!!, condContext)) continue
                        }
                        // The grant itself is credited to the replacement's source permanent.
                        val grantContext = EffectContext(
                            sourceId = sourceId,
                            controllerId = sourceControllerId,
                            affectedEntityId = enteringEntityId,
                        )
                        val (grantState, grantEvents) = grantKeywords(newState, effect, enteringEntityId, entityName, grantContext)
                        newState = grantState
                        events.addAll(grantEvents)
                    }
                    else -> { /* Other replacement effects handled elsewhere */ }
                }
            }
        }
        return newState to events
    }

    /**
     * Grant [effect]'s keywords to [enteringEntityId] as permanent floating effects. The grant
     * is entry-timestamped (Rule 613 layer ordering: a later "loses all abilities" removes it)
     * and cleaned up when the permanent leaves the battlefield
     * ([ZoneMovementUtils.removeFloatingEffectsTargeting], CR 400.7).
     */
    private fun grantKeywords(
        state: GameState,
        effect: EntersWithKeywords,
        enteringEntityId: EntityId,
        enteringEntityName: String,
        context: EffectContext,
    ): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()
        val sourceName = context.sourceId
            ?.let { newState.getEntity(it)?.get<CardComponent>()?.name }
            ?: enteringEntityName
        for (keyword in effect.keywords) {
            newState = newState.addFloatingEffect(
                layer = Layer.ABILITY,
                modification = SerializableModification.GrantKeyword(keyword.name),
                affectedEntities = setOf(enteringEntityId),
                duration = Duration.Permanent,
                context = context
            )
            events.add(
                KeywordGrantedEvent(
                    targetId = enteringEntityId,
                    targetName = enteringEntityName,
                    keyword = keyword.name.lowercase().replace('_', ' '),
                    sourceName = sourceName
                )
            )
        }
        return newState to events
    }

    /**
     * Does [enteringEntityId] match the `appliesTo` filter of a replacement carried by
     * [replacementSourceId]?
     *
     * `sourceId` in the predicate context is the **replacement's source**, not the entering
     * permanent: `appliesTo` describes the affected permanents *relative to the source*, so
     * source-relative predicates have to resolve against it — `HasChosenSubtype` reads the type
     * chosen as the source entered (Metallic Mimic), and `excludeSelf` means "other than the
     * source". (Conditions are the opposite and keep `sourceId = enteringEntityId`, because an
     * "enters with counters" condition describes the entering permanent — see the call sites.)
     */
    private fun matchesEnterFilter(
        event: com.wingedsheep.sdk.scripting.EventPattern,
        enteringEntityId: EntityId,
        replacementSourceId: EntityId,
        sourceControllerId: EntityId,
        state: GameState,
    ): Boolean {
        if (event !is com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent) return false
        if (event.to != Zone.BATTLEFIELD) return false
        val filter = event.filter

        val predicateContext = PredicateContext(
            sourceId = replacementSourceId,
            controllerId = sourceControllerId
        )
        return predicateEvaluator.matches(state, state.projectedState, enteringEntityId, filter, predicateContext)
    }

    fun resolveCounterType(filter: CounterTypeFilter): CounterType {
        return when (filter) {
            is CounterTypeFilter.Any -> CounterType.PLUS_ONE_PLUS_ONE
            is CounterTypeFilter.PlusOnePlusOne -> CounterType.PLUS_ONE_PLUS_ONE
            is CounterTypeFilter.MinusOneMinusOne -> CounterType.MINUS_ONE_MINUS_ONE
            is CounterTypeFilter.PlusOnePlusZero -> CounterType.PLUS_ONE_PLUS_ZERO
            is CounterTypeFilter.PlusZeroPlusOne -> CounterType.PLUS_ZERO_PLUS_ONE
            is CounterTypeFilter.MinusOneMinusZero -> CounterType.MINUS_ONE_MINUS_ZERO
            is CounterTypeFilter.MinusZeroMinusOne -> CounterType.MINUS_ZERO_MINUS_ONE
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

    /**
     * Apply a graveyard-cast entry rider (The Tomb of Aclazotz) to a permanent that just resolved
     * onto the battlefield after being cast from the graveyard under a rider-bearing
     * [com.wingedsheep.sdk.scripting.MayCastFromGraveyard] grant (frozen at cast time on the stack
     * spell as [com.wingedsheep.engine.state.components.stack.GraveyardCastRiderComponent]):
     *  - it enters with one [counter] counter (CR 614.1c), emitting a [CountersAddedEvent] so the
     *    finality death-replacement (ZoneMovementUtils) and counter watchers see it; and
     *  - it gains [addedSubtype] "in addition to its other types" via a floating [Layer.TYPE]
     *    modification with [Duration.Permanent] — floating effects targeting an entity are removed
     *    when it leaves the battlefield, giving exactly the "…while on the battlefield" persistence.
     * The subtype is applied only to creatures (CR 205.1b). Returns the new state and the events.
     */
    fun applyCastFromGraveyardRider(
        state: GameState,
        entityId: EntityId,
        controllerId: EntityId,
        counter: CounterType?,
        addedSubtype: String?
    ): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()
        if (counter != null) {
            val name = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: ""
            newState = newState.updateEntity(entityId) { c ->
                val current = c.get<CountersComponent>() ?: CountersComponent()
                c.with(current.withAdded(counter, 1))
            }
            events.add(CountersAddedEvent(entityId, counter.name, 1, name))
        }
        // Gate on the entering permanent's *base* type — the projected state isn't recomputed yet at
        // this ETB point, so it wouldn't yet see the just-resolved permanent as a creature.
        val entersAsCreature =
            newState.getEntity(entityId)?.get<CardComponent>()?.typeLine?.isCreature == true
        if (addedSubtype != null && entersAsCreature) {
            newState = newState.addFloatingEffect(
                layer = Layer.TYPE,
                modification = SerializableModification.AddSubtype(addedSubtype),
                affectedEntities = setOf(entityId),
                duration = Duration.Permanent,
                context = EffectContext(sourceId = entityId, controllerId = controllerId)
            )
        }
        return newState to events
    }
}
