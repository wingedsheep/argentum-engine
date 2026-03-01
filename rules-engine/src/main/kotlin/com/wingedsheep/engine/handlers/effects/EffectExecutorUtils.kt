package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.DamageDealtToCreaturesThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.TimestampComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent
import com.wingedsheep.engine.state.components.combat.DealtFirstStrikeDamageComponent
import com.wingedsheep.engine.state.components.combat.RequiresManualDamageAssignmentComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.ReplaceDamageWithCounters
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.PermanentsSacrificedEvent

/**
 * Utility functions shared across effect executors.
 */
object EffectExecutorUtils {

    private val stateProjector = StateProjector()
    private val predicateEvaluator = PredicateEvaluator()

    /**
     * Clean up combat references to a leaving entity on other creatures.
     * When a blocker leaves the battlefield, remove it from each attacker's
     * BlockedComponent.blockerIds and DamageAssignmentOrderComponent.orderedBlockers.
     */
    fun cleanupCombatReferences(state: GameState, leavingEntityId: EntityId): GameState {
        val container = state.getEntity(leavingEntityId) ?: return state
        val blockingComponent = container.get<BlockingComponent>() ?: return state

        var newState = state
        for (attackerId in blockingComponent.blockedAttackerIds) {
            newState = newState.updateEntity(attackerId) { attackerContainer ->
                var updated = attackerContainer
                val blocked = updated.get<BlockedComponent>()
                if (blocked != null) {
                    updated = updated.with(BlockedComponent(blocked.blockerIds.filter { it != leavingEntityId }))
                }
                val order = updated.get<DamageAssignmentOrderComponent>()
                if (order != null) {
                    updated = updated.with(DamageAssignmentOrderComponent(order.orderedBlockers.filter { it != leavingEntityId }))
                }
                updated
            }
        }
        return newState
    }

    /**
     * Remove floating effects targeting an entity that is leaving the battlefield.
     * Per MTG Rule 400.7, a card that changes zones becomes a new object with no
     * memory of its previous existence — all floating effects targeting it should be removed.
     *
     * For effects that target multiple entities, the leaving entity is removed from
     * affectedEntities but the effect is kept alive for the remaining targets.
     * Effects that exclusively target the leaving entity are removed entirely.
     */
    fun removeFloatingEffectsTargeting(state: GameState, entityId: EntityId): GameState {
        val updatedEffects = state.floatingEffects.mapNotNull { floatingEffect ->
            if (entityId !in floatingEffect.effect.affectedEntities) {
                floatingEffect
            } else {
                val remaining = floatingEffect.effect.affectedEntities - entityId
                if (remaining.isEmpty()) {
                    null // Remove entirely — this effect only targeted the leaving entity
                } else {
                    floatingEffect.copy(
                        effect = floatingEffect.effect.copy(affectedEntities = remaining)
                    )
                }
            }
        }
        return if (updatedEffects.size == state.floatingEffects.size &&
            updatedEffects.zip(state.floatingEffects).all { (a, b) -> a === b }) {
            state // No changes
        } else {
            state.copy(floatingEffects = updatedEffects)
        }
    }

    /**
     * Strip all battlefield-specific components from an entity leaving the battlefield.
     * Per MTG Rule 400.7, when an object changes zones it becomes a new object with no
     * memory of its previous existence. This removes all transient battlefield state:
     * tapped, damage, counters, summoning sickness, combat state, attachments, etc.
     */
    fun stripBattlefieldComponents(container: ComponentContainer): ComponentContainer {
        return container
            // Identity
            .without<ControllerComponent>()
            .without<FaceDownComponent>()
            .without<MorphDataComponent>()
            // Battlefield
            .without<TappedComponent>()
            .without<SummoningSicknessComponent>()
            .without<DamageComponent>()
            .without<DamageDealtToCreaturesThisTurnComponent>()
            .without<CountersComponent>()
            .without<AttachedToComponent>()
            .without<AttachmentsComponent>()
            .without<EnteredThisTurnComponent>()
            .without<ReplacementEffectSourceComponent>()
            .without<TimestampComponent>()
            // Combat
            .without<AttackingComponent>()
            .without<BlockingComponent>()
            .without<BlockedComponent>()
            .without<DamageAssignmentComponent>()
            .without<DamageAssignmentOrderComponent>()
            .without<DealtFirstStrikeDamageComponent>()
            .without<RequiresManualDamageAssignmentComponent>()
    }

    /**
     * Resolve a target from the effect target definition and context.
     */
    fun resolveTarget(effectTarget: EffectTarget, context: EffectContext): EntityId? {
        return when (effectTarget) {
            is EffectTarget.Self -> context.iterationTarget ?: context.sourceId
            is EffectTarget.Controller -> context.controllerId
            is EffectTarget.ContextTarget -> context.targets.getOrNull(effectTarget.index)?.toEntityId()
            is EffectTarget.BoundVariable -> context.namedTargets[effectTarget.name]?.toEntityId()
            is EffectTarget.SpecificEntity -> effectTarget.entityId
            is EffectTarget.TriggeringEntity -> context.triggeringEntityId
            is EffectTarget.PipelineTarget ->
                context.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index)
            else -> null
        }
    }

    /**
     * Resolve a target with access to game state (for targets like EnchantedCreature
     * that need to look up attachment relationships).
     */
    fun resolveTarget(effectTarget: EffectTarget, context: EffectContext, state: GameState): EntityId? {
        if (effectTarget is EffectTarget.EnchantedCreature) {
            val sourceId = context.sourceId ?: return null
            return state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId
        }
        if (effectTarget is EffectTarget.ControllerOfTriggeringEntity) {
            val triggerId = context.triggeringEntityId ?: return null
            val entity = state.getEntity(triggerId) ?: return null
            return entity.get<ControllerComponent>()?.playerId
                ?: entity.get<CardComponent>()?.ownerId
        }
        if (effectTarget is EffectTarget.PipelineTarget) {
            return context.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index)
        }
        return resolveTarget(effectTarget, context)
    }

    /**
     * Resolve a player target from the effect target definition and context.
     */
    fun resolvePlayerTarget(effectTarget: EffectTarget, context: EffectContext): EntityId? {
        return when (effectTarget) {
            is EffectTarget.Controller -> context.controllerId
            is EffectTarget.ContextTarget -> context.targets.getOrNull(effectTarget.index)?.toEntityId()
            is EffectTarget.BoundVariable -> context.namedTargets[effectTarget.name]?.toEntityId()
            is EffectTarget.PipelineTarget ->
                context.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index)
            is EffectTarget.PlayerRef -> when (effectTarget.player) {
                Player.You -> context.controllerId
                Player.Opponent, Player.TargetOpponent -> context.opponentId
                Player.TargetPlayer, Player.Any -> context.targets.firstOrNull()?.toEntityId()
                Player.TriggeringPlayer -> context.triggeringEntityId
                else -> null
            }
            else -> null
        }
    }

    /**
     * Resolve a player target with access to game state (for relational references like OwnerOf/ControllerOf).
     */
    fun resolvePlayerTarget(effectTarget: EffectTarget, context: EffectContext, state: GameState): EntityId? {
        // Try stateless resolution first
        resolvePlayerTarget(effectTarget, context)?.let { return it }

        // Handle state-dependent relational references
        return when (effectTarget) {
            is EffectTarget.PlayerRef -> when (val player = effectTarget.player) {
                is Player.OwnerOf -> {
                    val targetEntity = context.targets.firstOrNull()?.toEntityId() ?: return null
                    state.getEntity(targetEntity)?.get<CardComponent>()?.ownerId
                }
                is Player.ControllerOf -> {
                    val targetEntity = context.targets.firstOrNull()?.toEntityId() ?: return null
                    state.getEntity(targetEntity)?.get<ControllerComponent>()?.playerId
                        ?: state.getEntity(targetEntity)?.get<CardComponent>()?.ownerId
                }
                else -> null
            }
            else -> null
        }
    }

    /**
     * Resolve a player target to a list of player IDs (for multi-player effects like "each player").
     */
    fun resolvePlayerTargets(effectTarget: EffectTarget, state: GameState, context: EffectContext): List<EntityId> {
        return when (effectTarget) {
            is EffectTarget.Controller -> listOf(context.controllerId)
            is EffectTarget.BoundVariable -> context.namedTargets[effectTarget.name]?.toEntityId()?.let { listOf(it) } ?: emptyList()
            is EffectTarget.PipelineTarget -> {
                context.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index)
                    ?.let { listOf(it) } ?: emptyList()
            }
            is EffectTarget.PlayerRef -> when (effectTarget.player) {
                Player.Each -> state.turnOrder
                Player.EachOpponent -> state.turnOrder.filter { it != context.controllerId }
                Player.You -> listOf(context.controllerId)
                Player.Opponent, Player.TargetOpponent -> state.turnOrder.filter { it != context.controllerId }
                Player.TargetPlayer, Player.Any -> {
                    context.targets.firstOrNull()?.toEntityId()?.let { listOf(it) } ?: emptyList()
                }
                Player.TriggeringPlayer -> {
                    context.triggeringEntityId?.let { listOf(it) } ?: emptyList()
                }
                is Player.OwnerOf, is Player.ControllerOf -> {
                    resolvePlayerTarget(effectTarget, context, state)?.let { listOf(it) } ?: emptyList()
                }
                else -> emptyList()
            }
            else -> resolvePlayerTarget(effectTarget, context)?.let { listOf(it) } ?: emptyList()
        }
    }

    /**
     * Convert a ChosenTarget to an EntityId.
     */
    fun ChosenTarget.toEntityId(): EntityId = when (this) {
        is ChosenTarget.Player -> playerId
        is ChosenTarget.Permanent -> entityId
        is ChosenTarget.Card -> cardId
        is ChosenTarget.Spell -> spellEntityId
    }

    /**
     * Deal damage to a target (player or creature).
     *
     * @param state The current game state
     * @param targetId The entity to deal damage to
     * @param amount The amount of damage
     * @param sourceId The source of the damage
     * @param cantBePrevented If true, this damage cannot be prevented by prevention effects
     * @return The execution result with updated state and events
     */
    fun dealDamageToTarget(
        state: GameState,
        targetId: EntityId,
        amount: Int,
        sourceId: EntityId?,
        cantBePrevented: Boolean = false
    ): ExecutionResult {
        if (amount <= 0) return ExecutionResult.success(state)

        // Check for damage redirection (Glarecaster, Zealous Inquisitor)
        val (redirectState, redirectTargetId, redirectAmount) = checkDamageRedirection(state, targetId, amount)
        if (redirectTargetId != null) {
            val redirectResult = dealDamageToTarget(redirectState, redirectTargetId, redirectAmount, sourceId, cantBePrevented)
            val remainingDamage = amount - redirectAmount
            return if (remainingDamage > 0) {
                // Partial redirection — deal remaining damage to original target
                val afterRedirect = redirectResult.state
                val remainingResult = dealDamageToTarget(afterRedirect, targetId, remainingDamage, sourceId, cantBePrevented)
                ExecutionResult.success(remainingResult.state, redirectResult.events + remainingResult.events)
            } else {
                redirectResult
            }
        }

        // Protection from color/subtype: damage from sources of the stated quality is prevented (Rule 702.16)
        if (!cantBePrevented && sourceId != null) {
            // Check if all damage from this source is prevented (Chain of Silence)
            if (isAllDamageFromSourcePrevented(state, sourceId)) {
                return ExecutionResult.success(state)
            }

            val projected = stateProjector.project(state)
            val sourceColors = projected.getColors(sourceId)
            for (colorName in sourceColors) {
                if (projected.hasKeyword(targetId, "PROTECTION_FROM_$colorName")) {
                    // Damage is prevented — return success with no state change
                    return ExecutionResult.success(state)
                }
            }
            val sourceSubtypes = projected.getSubtypes(sourceId)
            for (subtype in sourceSubtypes) {
                if (projected.hasKeyword(targetId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")) {
                    return ExecutionResult.success(state)
                }
            }
        }

        // Apply damage amplification (e.g., Gratuitous Violence - DoubleDamage)
        var effectiveAmount = applyStaticDamageAmplification(state, targetId, amount, sourceId)
        var newState = state

        // Check for damage-to-counters replacement (Force Bubble)
        // This replaces the damage entirely — it is neither dealt nor prevented.
        val isPlayer = newState.getEntity(targetId)?.get<LifeTotalComponent>() != null
        if (isPlayer) {
            val counterResult = applyReplaceDamageWithCounters(newState, targetId, effectiveAmount, sourceId)
            if (counterResult != null) return counterResult
        }

        if (!cantBePrevented) {
            val (shieldState, reducedAmount) = applyDamagePreventionShields(newState, targetId, effectiveAmount)
            newState = shieldState
            effectiveAmount = reducedAmount
        }
        if (effectiveAmount <= 0) return ExecutionResult.success(newState)

        val events = mutableListOf<EngineGameEvent>()

        // Check if target is a player or creature
        val lifeComponent = newState.getEntity(targetId)?.get<LifeTotalComponent>()
        if (lifeComponent != null) {
            // It's a player - reduce life
            val newLife = lifeComponent.life - effectiveAmount
            newState = newState.updateEntity(targetId) { container ->
                container.with(LifeTotalComponent(newLife))
            }
            newState = trackDamageReceivedByPlayer(newState, targetId, effectiveAmount)
            events.add(LifeChangedEvent(targetId, lifeComponent.life, newLife, LifeChangeReason.DAMAGE))
        } else {
            // It's a creature - mark damage
            val currentDamage = newState.getEntity(targetId)?.get<DamageComponent>()?.amount ?: 0
            newState = newState.updateEntity(targetId) { container ->
                container.with(DamageComponent(currentDamage + effectiveAmount))
            }
            // Track damage source for "creature dealt damage by this dies" triggers
            if (sourceId != null) {
                newState = trackDamageDealtToCreature(newState, sourceId, targetId)
            }
        }

        val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val targetName = newState.getEntity(targetId)?.get<CardComponent>()?.name
        val targetIsPlayer = newState.getEntity(targetId)?.get<LifeTotalComponent>() != null
        events.add(DamageDealtEvent(sourceId, targetId, effectiveAmount, false, sourceName = sourceName, targetName = targetName, targetIsPlayer = targetIsPlayer))

        return ExecutionResult.success(newState, events)
    }

    /**
     * Track that [playerId] received [amount] damage this turn.
     * Updates the DamageReceivedThisTurnComponent on the player entity.
     * Used for Final Punishment: "Target player loses life equal to the damage
     * already dealt to that player this turn."
     */
    fun trackDamageReceivedByPlayer(state: GameState, playerId: EntityId, amount: Int): GameState {
        if (amount <= 0) return state
        return state.updateEntity(playerId) { container ->
            val existing = container.get<com.wingedsheep.engine.state.components.player.DamageReceivedThisTurnComponent>()
                ?: com.wingedsheep.engine.state.components.player.DamageReceivedThisTurnComponent()
            container.with(com.wingedsheep.engine.state.components.player.DamageReceivedThisTurnComponent(existing.amount + amount))
        }
    }

    /**
     * Track that [sourceId] dealt damage to [targetCreatureId] this turn.
     * Updates the DamageDealtToCreaturesThisTurnComponent on the source entity.
     * Used for triggers like Soul Collector's "whenever a creature dealt damage by this creature this turn dies".
     */
    fun trackDamageDealtToCreature(state: GameState, sourceId: EntityId, targetCreatureId: EntityId): GameState {
        // Only track if source is still on the battlefield
        if (sourceId !in state.getBattlefield()) return state
        return state.updateEntity(sourceId) { container ->
            val existing = container.get<DamageDealtToCreaturesThisTurnComponent>()
                ?: DamageDealtToCreaturesThisTurnComponent()
            container.with(existing.withCreature(targetCreatureId))
        }
    }

    /**
     * Destroy a permanent (move to graveyard).
     *
     * @param state The current game state
     * @param entityId The entity to destroy
     * @param canRegenerate If false, regeneration shields are not checked (e.g. Wrath of God)
     * @return The execution result with updated state and events
     */
    fun destroyPermanent(state: GameState, entityId: EntityId, canRegenerate: Boolean = true): ExecutionResult {
        val container = state.getEntity(entityId)
            ?: return ExecutionResult.error(state, "Entity not found: $entityId")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card: $entityId")

        // Check for indestructible - indestructible permanents can't be destroyed
        if (stateProjector.hasProjectedKeyword(state, entityId, Keyword.INDESTRUCTIBLE)) {
            // Indestructible - the destroy effect has no effect (not an error)
            return ExecutionResult.success(state)
        }

        // Check for regeneration shields
        if (canRegenerate) {
            val (shieldState, wasRegenerated) = applyRegenerationShields(state, entityId)
            if (wasRegenerated) {
                return applyRegenerationReplacement(shieldState, entityId)
            }
        }

        // Find which player's battlefield it's on
        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.error(state, "Cannot determine owner")

        val ownerId = cardComponent.ownerId ?: controllerId

        // Move to graveyard
        val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
        val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

        var newState = state.removeFromZone(battlefieldZone, entityId)
        newState = newState.addToZone(graveyardZone, entityId)

        // Clean up combat references before stripping components
        newState = cleanupCombatReferences(newState, entityId)

        // Remove permanent-only components
        newState = newState.updateEntity(entityId) { c -> stripBattlefieldComponents(c) }

        // Remove floating effects targeting this entity (Rule 400.7)
        newState = removeFloatingEffectsTargeting(newState, entityId)

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId,
                    cardComponent.name,
                    Zone.BATTLEFIELD,
                    Zone.GRAVEYARD,
                    ownerId
                )
            )
        )
    }

    /**
     * Move a card from any zone to another zone.
     * Finds the card's current zone and moves it to the destination.
     *
     * @param state The current game state
     * @param entityId The entity to move
     * @param targetZone The destination zone type
     * @return The execution result with updated state and events
     */
    fun moveCardToZone(state: GameState, entityId: EntityId, targetZone: Zone): ExecutionResult {
        val container = state.getEntity(entityId)
            ?: return ExecutionResult.error(state, "Entity not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        val ownerId = cardComponent.ownerId
            ?: return ExecutionResult.error(state, "Cannot determine owner")

        // Find the current zone of the card
        val currentZone = state.zones.entries.find { (_, cards) -> entityId in cards }?.key
            ?: return ExecutionResult.error(state, "Card not in any zone")

        // Move to target zone
        val targetZoneKey = ZoneKey(ownerId, targetZone)

        var newState = state.removeFromZone(currentZone, entityId)
        newState = newState.addToZone(targetZoneKey, entityId)

        // Clean up combat references and remove permanent-only components if moving from battlefield
        if (currentZone.zoneType == Zone.BATTLEFIELD) {
            newState = cleanupCombatReferences(newState, entityId)
            newState = newState.updateEntity(entityId) { c -> stripBattlefieldComponents(c) }
            newState = removeFloatingEffectsTargeting(newState, entityId)
        }

        // Add controller component when moving to battlefield
        if (targetZone == Zone.BATTLEFIELD) {
            newState = newState.updateEntity(entityId) { c ->
                c.with(ControllerComponent(ownerId))
                    .with(SummoningSicknessComponent)
            }
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId,
                    cardComponent.name,
                    currentZone.zoneType,
                    targetZone,
                    ownerId
                )
            )
        )
    }

    /**
     * Apply damage prevention shields to reduce incoming damage.
     *
     * Finds all PreventNextDamage floating effects targeting the entity,
     * consumes shield amounts, and returns the updated state and remaining damage.
     *
     * @param state The current game state
     * @param targetId The entity receiving damage
     * @param amount The incoming damage amount
     * @return Pair of (updated state with consumed shields, remaining damage after prevention)
     */
    fun applyDamagePreventionShields(
        state: GameState,
        targetId: EntityId,
        amount: Int,
        isCombatDamage: Boolean = false,
        sourceId: EntityId? = null
    ): Pair<GameState, Int> {
        var remainingDamage = amount
        val updatedEffects = state.floatingEffects.toMutableList()
        val toRemove = mutableListOf<Int>()

        for (i in updatedEffects.indices) {
            if (remainingDamage <= 0) break
            val effect = updatedEffects[i]
            val mod = effect.effect.modification
            if (mod is SerializableModification.PreventNextDamage && targetId in effect.effect.affectedEntities) {
                // Source-specific shields (from CR 615.7 prevention distribution) only match their source
                if (mod.onlyFromSource != null && mod.onlyFromSource != sourceId) continue
                val prevented = minOf(mod.remainingAmount, remainingDamage)
                remainingDamage -= prevented
                val newRemaining = mod.remainingAmount - prevented
                if (newRemaining <= 0) {
                    toRemove.add(i)
                } else {
                    updatedEffects[i] = effect.copy(
                        effect = effect.effect.copy(
                            modification = SerializableModification.PreventNextDamage(newRemaining, mod.onlyFromSource)
                        )
                    )
                }
            }
        }

        // Check for creature-type-specific prevention shields (Circle of Solace)
        if (remainingDamage > 0 && sourceId != null) {
            val projected = stateProjector.project(state)
            val sourceSubtypes = projected.getSubtypes(sourceId).map { it.uppercase() }.toSet()
            val sourceCard = state.getEntity(sourceId)?.get<CardComponent>()
            if (sourceCard != null && sourceCard.isCreature) {
                for (i in updatedEffects.indices) {
                    if (remainingDamage <= 0) break
                    if (i in toRemove) continue
                    val effect = updatedEffects[i]
                    val mod = effect.effect.modification
                    if (mod is SerializableModification.PreventNextDamageFromCreatureType &&
                        targetId in effect.effect.affectedEntities &&
                        mod.creatureType.uppercase() in sourceSubtypes
                    ) {
                        // Prevent all damage from this instance and consume the shield
                        remainingDamage = 0
                        toRemove.add(i)
                    }
                }
            }
        }

        // Remove fully consumed shields in reverse order to maintain indices
        for (idx in toRemove.sortedDescending()) {
            updatedEffects.removeAt(idx)
        }

        var newState = state.copy(floatingEffects = updatedEffects)

        // Apply static damage reduction from permanents with ReplacementEffectSourceComponent
        remainingDamage = applyStaticDamageReduction(newState, targetId, remainingDamage, isCombatDamage, sourceId)

        return newState to remainingDamage
    }

    /**
     * Check if all damage from a specific source creature is prevented this turn.
     * Used by Chain of Silence and similar "prevent all damage creature would deal" effects.
     */
    fun isAllDamageFromSourcePrevented(state: GameState, sourceId: EntityId): Boolean {
        return state.floatingEffects.any { floatingEffect ->
            floatingEffect.effect.modification is SerializableModification.PreventAllDamageDealtBy &&
                sourceId in floatingEffect.effect.affectedEntities
        }
    }

    /**
     * Check for damage redirection shields (Glarecaster, Zealous Inquisitor).
     *
     * Scans floating effects for RedirectNextDamage targeting the entity.
     * If found, consumes (or decrements) the shield and returns the redirect target ID
     * and the amount to redirect.
     *
     * @param state The current game state
     * @param targetId The entity about to receive damage
     * @param damageAmount The amount of damage about to be dealt
     * @return Triple of (updated state with consumed/decremented shield, redirect target ID or null, amount to redirect)
     */
    fun checkDamageRedirection(state: GameState, targetId: EntityId, damageAmount: Int): Triple<GameState, EntityId?, Int> {
        val shieldIndex = state.floatingEffects.indexOfFirst { effect ->
            effect.effect.modification is SerializableModification.RedirectNextDamage &&
                targetId in effect.effect.affectedEntities
        }
        if (shieldIndex == -1) return Triple(state, null, 0)

        val shield = state.floatingEffects[shieldIndex]
        val mod = shield.effect.modification as SerializableModification.RedirectNextDamage

        val redirectAmount = if (mod.amount != null) minOf(mod.amount, damageAmount) else damageAmount

        val updatedEffects = state.floatingEffects.toMutableList()
        if (mod.amount != null) {
            val remaining = mod.amount - redirectAmount
            if (remaining <= 0) {
                // Shield fully consumed
                updatedEffects.removeAt(shieldIndex)
            } else {
                // Decrement the shield
                updatedEffects[shieldIndex] = shield.copy(
                    effect = shield.effect.copy(
                        modification = mod.copy(amount = remaining)
                    )
                )
            }
        } else {
            // No amount limit — consume the whole shield
            updatedEffects.removeAt(shieldIndex)
        }

        return Triple(state.copy(floatingEffects = updatedEffects), mod.redirectToId, redirectAmount)
    }

    /**
     * Apply static damage reduction from permanents on the battlefield.
     *
     * Scans all battlefield entities for ReplacementEffectSourceComponent containing
     * PreventDamage effects, and reduces damage if the target matches the effect's filter.
     *
     * @param state The current game state
     * @param targetId The entity receiving damage
     * @param amount The incoming damage amount
     * @param isCombatDamage Whether this is combat damage (for DamageType filtering)
     * @param sourceId The entity dealing damage (for source-based prevention like Sandskin)
     * @return The reduced damage amount (minimum 0)
     */
    private fun applyStaticDamageReduction(
        state: GameState,
        targetId: EntityId,
        amount: Int,
        isCombatDamage: Boolean = false,
        sourceId: EntityId? = null
    ): Int {
        if (amount <= 0) return 0

        var remainingDamage = amount
        val projected = stateProjector.project(state)

        for (entityId in state.getBattlefield()) {
            if (remainingDamage <= 0) break
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = container.get<ControllerComponent>()?.playerId ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (remainingDamage <= 0) break
                if (effect !is PreventDamage) continue

                val damageEvent = effect.appliesTo
                if (damageEvent !is com.wingedsheep.sdk.scripting.GameEvent.DamageEvent) continue

                // Check damage type filter (combat vs non-combat)
                val damageTypeMatches = when (damageEvent.damageType) {
                    is DamageType.Any -> true
                    is DamageType.Combat -> isCombatDamage
                    is DamageType.NonCombat -> !isCombatDamage
                }
                if (!damageTypeMatches) continue

                // Check if the damage source matches the source filter
                val sourceMatches = when (val source = damageEvent.source) {
                    is SourceFilter.Any -> true
                    is SourceFilter.EnchantedCreature -> {
                        val attachedTo = container.get<AttachedToComponent>()?.targetId
                        sourceId != null && sourceId == attachedTo
                    }
                    is SourceFilter.Matching -> {
                        if (sourceId == null) false
                        else {
                            val context = PredicateContext(controllerId = sourceControllerId)
                            predicateEvaluator.matchesWithProjection(state, projected, sourceId, source.filter, context)
                        }
                    }
                    else -> false
                }
                if (!sourceMatches) continue

                // Check if the target matches the recipient filter
                val recipientMatches = when (val recipient = damageEvent.recipient) {
                    is RecipientFilter.Self -> targetId == entityId
                    is RecipientFilter.EnchantedCreature -> {
                        val attachedTo = container.get<AttachedToComponent>()?.targetId
                        targetId == attachedTo
                    }
                    is RecipientFilter.Matching -> {
                        val context = PredicateContext(controllerId = sourceControllerId)
                        predicateEvaluator.matchesWithProjection(state, projected, targetId, recipient.filter, context)
                    }
                    is RecipientFilter.CreatureYouControl -> {
                        val isCreature = state.getEntity(targetId)?.get<CardComponent>()?.typeLine?.isCreature == true
                        val isControlled = state.getEntity(targetId)?.get<ControllerComponent>()?.playerId == sourceControllerId
                        isCreature && isControlled
                    }
                    is RecipientFilter.Any -> true
                    else -> false
                }

                if (recipientMatches) {
                    val preventAmount = effect.amount
                    val prevented = if (preventAmount == null) remainingDamage else minOf(preventAmount, remainingDamage)
                    remainingDamage -= prevented
                }
            }
        }

        return remainingDamage.coerceAtLeast(0)
    }

    /**
     * Apply static damage amplification from permanents on the battlefield.
     *
     * Scans all battlefield entities for ReplacementEffectSourceComponent containing
     * DoubleDamage effects, and doubles damage if the source and recipient match.
     * Per MTG rules, damage amplification applies before prevention.
     *
     * @param state The current game state
     * @param targetId The entity receiving damage
     * @param amount The incoming damage amount
     * @param sourceId The entity dealing damage
     * @return The amplified damage amount
     */
    fun applyStaticDamageAmplification(
        state: GameState,
        targetId: EntityId,
        amount: Int,
        sourceId: EntityId?
    ): Int {
        if (amount <= 0) return 0

        var amplifiedAmount = amount
        val projected = stateProjector.project(state)

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = container.get<ControllerComponent>()?.playerId ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is DoubleDamage) continue

                val damageEvent = effect.appliesTo
                if (damageEvent !is com.wingedsheep.sdk.scripting.GameEvent.DamageEvent) continue

                // Check if the damage source matches the source filter
                val sourceMatches = when (val sourceFilter = damageEvent.source) {
                    is SourceFilter.Any -> true
                    is SourceFilter.Matching -> {
                        if (sourceId == null) false
                        else {
                            val context = PredicateContext(controllerId = sourceControllerId)
                            predicateEvaluator.matchesWithProjection(state, projected, sourceId, sourceFilter.filter, context)
                        }
                    }
                    else -> false
                }
                if (!sourceMatches) continue

                // Check if the target matches the recipient filter
                val recipientMatches = when (val recipient = damageEvent.recipient) {
                    is RecipientFilter.Any -> true
                    is RecipientFilter.Matching -> {
                        val context = PredicateContext(controllerId = sourceControllerId)
                        predicateEvaluator.matchesWithProjection(state, projected, targetId, recipient.filter, context)
                    }
                    is RecipientFilter.CreatureYouControl -> {
                        val isCreature = state.getEntity(targetId)?.get<CardComponent>()?.typeLine?.isCreature == true
                        val isControlled = state.getEntity(targetId)?.get<ControllerComponent>()?.playerId == sourceControllerId
                        isCreature && isControlled
                    }
                    else -> false
                }
                if (!recipientMatches) continue

                amplifiedAmount *= 2
            }
        }

        return amplifiedAmount
    }

    /**
     * Check for ReplaceDamageWithCounters replacement effects (Force Bubble).
     *
     * Scans the battlefield for permanents with ReplaceDamageWithCounters replacement
     * effects. If found and the recipient matches, replaces all damage with counters
     * on the source permanent. If the counter threshold is met, sacrifices the permanent.
     *
     * @param state The current game state
     * @param targetId The player entity receiving damage
     * @param amount The damage amount to replace
     * @param sourceId The entity dealing damage (for source filtering)
     * @return ExecutionResult if replacement was applied, null if no replacement found
     */
    fun applyReplaceDamageWithCounters(
        state: GameState,
        targetId: EntityId,
        amount: Int,
        sourceId: EntityId?
    ): ExecutionResult? {
        if (amount <= 0) return null

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = container.get<ControllerComponent>()?.playerId ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is ReplaceDamageWithCounters) continue

                val damageEvent = effect.appliesTo
                if (damageEvent !is com.wingedsheep.sdk.scripting.GameEvent.DamageEvent) continue

                // Check recipient filter
                val recipientMatches = when (val recipient = damageEvent.recipient) {
                    is RecipientFilter.You -> targetId == sourceControllerId
                    is RecipientFilter.Any -> true
                    else -> false
                }
                if (!recipientMatches) continue

                // Match found — replace damage with counters on this permanent
                val events = mutableListOf<EngineGameEvent>()
                var newState = state

                // Convert string counter type to CounterType enum
                val counterType = try {
                    CounterType.valueOf(
                        effect.counterType.uppercase()
                            .replace(' ', '_')
                            .replace('+', 'P')
                            .replace('-', 'M')
                            .replace("/", "_")
                    )
                } catch (e: IllegalArgumentException) {
                    CounterType.PLUS_ONE_PLUS_ONE
                }

                // Add counters to the enchantment
                val currentCounters = container.get<CountersComponent>() ?: CountersComponent()
                val updatedCounters = currentCounters.withAdded(counterType, amount)
                newState = newState.updateEntity(entityId) { c ->
                    c.with(updatedCounters)
                }

                val entityName = container.get<CardComponent>()?.name ?: ""
                events.add(CountersAddedEvent(entityId, effect.counterType, amount, entityName))

                // Check sacrifice threshold (state-triggered ability approximation)
                val totalCounters = updatedCounters.getCount(counterType)
                val threshold = effect.sacrificeThreshold
                if (threshold != null && totalCounters >= threshold) {
                    val ownerId = container.get<CardComponent>()?.ownerId ?: sourceControllerId
                    val zoneKey = state.zones.entries.find { (_, cards) -> entityId in cards }?.key
                    if (zoneKey != null) {
                        newState = newState.removeFromZone(zoneKey, entityId)
                        val graveyardKey = ZoneKey(ownerId, Zone.GRAVEYARD)
                        newState = newState.addToZone(graveyardKey, entityId)
                        newState = newState.updateEntity(entityId) { c -> stripBattlefieldComponents(c) }
                        newState = removeFloatingEffectsTargeting(newState, entityId)
                        events.add(
                            PermanentsSacrificedEvent(
                                sourceControllerId,
                                listOf(entityId),
                                listOf(entityName)
                            )
                        )
                        events.add(
                            ZoneChangeEvent(
                                entityId,
                                entityName,
                                Zone.BATTLEFIELD,
                                Zone.GRAVEYARD,
                                ownerId
                            )
                        )
                    }
                }

                return ExecutionResult.success(newState, events)
            }
        }

        return null
    }

    /**
     * Check for regeneration shields on an entity and consume one if found.
     *
     * Regeneration shields are floating effects with RegenerationShield modification.
     * If a CantBeRegenerated floating effect targets the entity, regeneration is blocked.
     *
     * @param state The current game state
     * @param entityId The entity being destroyed
     * @return Pair of (updated state with consumed shield, whether regeneration occurred)
     */
    fun applyRegenerationShields(state: GameState, entityId: EntityId): Pair<GameState, Boolean> {
        // Check if the entity has a CantBeRegenerated floating effect
        val cantRegenerate = state.floatingEffects.any { effect ->
            effect.effect.modification is SerializableModification.CantBeRegenerated &&
                entityId in effect.effect.affectedEntities
        }
        if (cantRegenerate) return state to false

        // Find the first regeneration shield targeting this entity
        val shieldIndex = state.floatingEffects.indexOfFirst { effect ->
            effect.effect.modification is SerializableModification.RegenerationShield &&
                entityId in effect.effect.affectedEntities
        }
        if (shieldIndex == -1) return state to false

        // Consume the shield (remove it)
        val updatedEffects = state.floatingEffects.toMutableList()
        updatedEffects.removeAt(shieldIndex)

        return state.copy(floatingEffects = updatedEffects) to true
    }

    /**
     * Apply regeneration replacement effect: tap the creature, remove all damage,
     * and remove it from combat. The creature stays on the battlefield.
     *
     * @param state The current game state (shield already consumed)
     * @param entityId The entity being regenerated
     * @return ExecutionResult with the regenerated creature still on the battlefield
     */
    fun applyRegenerationReplacement(state: GameState, entityId: EntityId): ExecutionResult {
        val newState = state.updateEntity(entityId) { c ->
            c.with(TappedComponent)
                .without<DamageComponent>()
                .without<AttackingComponent>()
                .without<BlockingComponent>()
                .without<BlockedComponent>()
                .without<DamageAssignmentComponent>()
                .without<DamageAssignmentOrderComponent>()
        }
        return ExecutionResult.success(newState)
    }

    /**
     * Move a permanent from battlefield to another zone, cleaning up permanent-only components.
     *
     * @param state The current game state
     * @param entityId The entity to move
     * @param targetZone The destination zone type
     * @return The execution result with updated state and events
     */
    fun movePermanentToZone(state: GameState, entityId: EntityId, targetZone: Zone): ExecutionResult {
        val container = state.getEntity(entityId)
            ?: return ExecutionResult.error(state, "Entity not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.error(state, "Cannot determine owner")

        val ownerId = cardComponent.ownerId ?: controllerId

        // Move to target zone
        val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
        val targetZoneKey = ZoneKey(ownerId, targetZone)

        var newState = state.removeFromZone(battlefieldZone, entityId)
        newState = newState.addToZone(targetZoneKey, entityId)

        // Remove permanent-only components
        newState = newState.updateEntity(entityId) { c -> stripBattlefieldComponents(c) }

        // Remove floating effects targeting this entity (Rule 400.7)
        newState = removeFloatingEffectsTargeting(newState, entityId)

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId,
                    cardComponent.name,
                    Zone.BATTLEFIELD,
                    targetZone,
                    ownerId
                )
            )
        )
    }
}
