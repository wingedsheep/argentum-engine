package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.DamageDealtToCreaturesThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtCombatDamageToPlayerComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtDamageComponent
import com.wingedsheep.engine.state.components.battlefield.WasDealtDamageThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.ExileOnLeaveBattlefieldComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.battlefield.CastFromHandComponent
import com.wingedsheep.engine.state.components.battlefield.WarpedComponent
import com.wingedsheep.engine.state.components.battlefield.WasKickedComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.TimestampComponent
import com.wingedsheep.engine.state.components.combat.AttackerOrderComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent
import com.wingedsheep.engine.state.components.combat.DealtFirstStrikeDamageComponent
import com.wingedsheep.engine.state.components.combat.RequiresManualDamageAssignmentComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWithEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Result of a zone change redirect check.
 *
 * @param destinationZone The (possibly redirected) destination zone
 * @param additionalEffect An optional effect to execute when the redirect applies
 *        (e.g., TakeExtraTurnEffect from Ugin's Nexus)
 * @param effectControllerId The controller of the replacement effect source
 */
data class ZoneChangeRedirectResult(
    val destinationZone: Zone,
    val additionalEffect: com.wingedsheep.sdk.scripting.effects.Effect? = null,
    val effectControllerId: EntityId? = null
)

/**
 * Utility functions for zone transitions: moving permanents between zones,
 * destroying permanents, and cleaning up battlefield state.
 *
 * Handles combat reference cleanup, attachment cleanup, floating effect removal,
 * component stripping, zone change redirection, and regeneration.
 */
object ZoneMovementUtils {

    private val predicateEvaluator = PredicateEvaluator()

    /**
     * Apply Saga entry setup to an entity entering the battlefield (Rule 714.3a).
     * Adds SagaComponent with chapter 1 marked as triggered, and adds an initial lore counter.
     *
     * @return Pair of (updated state, list of events to emit) — empty events if not a Saga
     */
    fun applySagaEntryIfNeeded(
        state: GameState,
        entityId: EntityId
    ): Pair<GameState, List<EngineGameEvent>> {
        val container = state.getEntity(entityId) ?: return state to emptyList()
        val cardComponent = container.get<CardComponent>() ?: return state to emptyList()
        if (!cardComponent.typeLine.isSaga) return state to emptyList()

        val current = container.get<CountersComponent>() ?: CountersComponent()
        val sagaComponent = SagaComponent(triggeredChapters = setOf(1))
        val newState = state.updateEntity(entityId) { c ->
            c.with(sagaComponent)
                .with(current.withAdded(CounterType.LORE, 1))
        }
        return newState to listOf(CountersAddedEvent(entityId, "LORE", 1, cardComponent.name))
    }

    /**
     * Clean up combat references to a leaving entity on other creatures.
     * When a blocker leaves the battlefield, remove it from each attacker's
     * BlockedComponent.blockerIds and DamageAssignmentOrderComponent.orderedBlockers.
     * When an attacker leaves the battlefield, remove it from each blocker's
     * BlockingComponent.blockedAttackerIds and AttackerOrderComponent.orderedAttackers.
     */
    fun cleanupCombatReferences(state: GameState, leavingEntityId: EntityId): GameState {
        val container = state.getEntity(leavingEntityId) ?: return state
        var newState = state

        // If leaving entity is a blocker, clean up attackers' references to it
        val blockingComponent = container.get<BlockingComponent>()
        if (blockingComponent != null) {
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
        }

        // If leaving entity is an attacker, clean up blockers' references to it
        val attackingComponent = container.get<AttackingComponent>()
        if (attackingComponent != null) {
            for ((entityId, components) in newState.entities) {
                val blocking = components.get<BlockingComponent>() ?: continue
                if (leavingEntityId in blocking.blockedAttackerIds) {
                    newState = newState.updateEntity(entityId) { blockerContainer ->
                        var updated = blockerContainer
                        val updatedBlocking = updated.get<BlockingComponent>()
                        if (updatedBlocking != null) {
                            val updatedIds = updatedBlocking.blockedAttackerIds - leavingEntityId
                            updated = if (updatedIds.isEmpty()) {
                                updated.without<BlockingComponent>()
                            } else {
                                updated.with(BlockingComponent(updatedIds))
                            }
                        }
                        val attackerOrder = updated.get<AttackerOrderComponent>()
                        if (attackerOrder != null) {
                            updated = updated.with(AttackerOrderComponent(attackerOrder.orderedAttackers.filter { it != leavingEntityId }))
                        }
                        updated
                    }
                }
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
     * Clean up the reverse attachment link (AttachmentsComponent) on the permanent
     * that this entity was attached to. Called when an aura or equipment leaves the battlefield.
     */
    fun cleanupReverseAttachmentLink(state: GameState, entityId: EntityId): GameState {
        val attachedTo = state.getEntity(entityId)?.get<AttachedToComponent>() ?: return state
        return state.updateEntity(attachedTo.targetId) { container ->
            val attachments = container.get<AttachmentsComponent>()
            if (attachments != null) {
                val updatedIds = attachments.attachedIds.filter { it != entityId }
                if (updatedIds.isEmpty()) {
                    container.without<AttachmentsComponent>()
                } else {
                    container.with(AttachmentsComponent(updatedIds))
                }
            } else {
                container
            }
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
            .without<RevealedToComponent>()
            // Battlefield
            .without<TappedComponent>()
            .without<SummoningSicknessComponent>()
            .without<CastFromHandComponent>()
            .without<WasKickedComponent>()
            .without<WarpedComponent>()
            .without<DamageComponent>()
            .without<DamageDealtToCreaturesThisTurnComponent>()
            .without<WasDealtDamageThisTurnComponent>()
            .without<HasDealtDamageComponent>()
            .without<HasDealtCombatDamageToPlayerComponent>()
            .without<CountersComponent>()
            .without<AttachedToComponent>()
            .without<AttachmentsComponent>()
            .without<EnteredThisTurnComponent>()
            .without<ExileOnLeaveBattlefieldComponent>()
            .without<SagaComponent>()
            .without<ReplacementEffectSourceComponent>()
            .without<TimestampComponent>()
            // Combat
            .without<AttackingComponent>()
            .without<BlockingComponent>()
            .without<BlockedComponent>()
            .without<DamageAssignmentComponent>()
            .without<DamageAssignmentOrderComponent>()
            .without<AttackerOrderComponent>()
            .without<DealtFirstStrikeDamageComponent>()
            .without<RequiresManualDamageAssignmentComponent>()
    }

    /**
     * Destroy a permanent (move to graveyard).
     *
     * @param state The current game state
     * @param entityId The entity to destroy
     * @param canRegenerate If false, regeneration shields are not checked (e.g. Wrath of God)
     * @return The execution result with updated state and events
     */
    fun destroyPermanent(state: GameState, entityId: EntityId, canRegenerate: Boolean = true): EffectResult {
        val container = state.getEntity(entityId)
            ?: return EffectResult.error(state, "Entity not found: $entityId")

        container.get<CardComponent>()
            ?: return EffectResult.error(state, "Not a card: $entityId")

        // Check for indestructible - indestructible permanents can't be destroyed
        if (state.projectedState.hasKeyword(entityId, Keyword.INDESTRUCTIBLE)) {
            return EffectResult.success(state)
        }

        // Check for regeneration shields
        if (canRegenerate) {
            val (shieldState, wasRegenerated) = applyRegenerationShields(state, entityId)
            if (wasRegenerated) {
                return applyRegenerationReplacement(shieldState, entityId)
            }
        }

        // Delegate to ZoneTransitionService
        val result = ZoneTransitionService.moveToZone(state, entityId, Zone.GRAVEYARD)
        return EffectResult.success(result.state, result.events)
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
    fun moveCardToZone(state: GameState, entityId: EntityId, targetZone: Zone): EffectResult {
        val container = state.getEntity(entityId)
            ?: return EffectResult.error(state, "Entity not found")

        container.get<CardComponent>()
            ?: return EffectResult.error(state, "Not a card")

        val result = ZoneTransitionService.moveToZone(state, entityId, targetZone)
        return EffectResult.success(result.state, result.events)
    }

    /**
     * Check if a zone change to graveyard should be redirected to another zone
     * by a RedirectZoneChange replacement effect on the battlefield.
     *
     * For example, Anafenza, the Foremost exiles opponent's nontoken creature cards
     * instead of letting them go to the graveyard.
     *
     * @param state The current game state
     * @param entityId The entity about to change zones
     * @param fromZone The zone the entity is coming from (null if unknown)
     * @param toZone The intended destination zone
     * @return The redirect result with destination zone and any additional effects
     */
    fun checkZoneChangeRedirect(
        state: GameState,
        entityId: EntityId,
        fromZone: Zone?,
        toZone: Zone
    ): ZoneChangeRedirectResult {
        val container = state.getEntity(entityId) ?: return ZoneChangeRedirectResult(toZone)

        // Check if the entity itself has ExileOnLeaveBattlefieldComponent
        // (e.g., creature returned by Kheru Lich Lord, Whip of Erebos)
        if (fromZone == Zone.BATTLEFIELD && toZone != Zone.EXILE &&
            container.has<ExileOnLeaveBattlefieldComponent>()
        ) {
            return ZoneChangeRedirectResult(Zone.EXILE)
        }

        // Check for finality counter — if a permanent with a finality counter
        // would die (go from battlefield to graveyard), exile it instead.
        if (fromZone == Zone.BATTLEFIELD && toZone == Zone.GRAVEYARD) {
            val counters = container.get<CountersComponent>()
            if (counters != null && counters.getCount(CounterType.FINALITY) > 0) {
                return ZoneChangeRedirectResult(Zone.EXILE)
            }
        }

        for (permanentId in state.getBattlefield()) {
            val permContainer = state.getEntity(permanentId) ?: continue
            val replacementComponent = permContainer.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = permContainer.get<ControllerComponent>()?.playerId ?: continue

            for (effect in replacementComponent.replacementEffects) {
                when (effect) {
                    is RedirectZoneChange -> {
                        val event = effect.appliesTo
                        if (event !is com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent) continue

                        // Check destination zone matches
                        if (event.to != null && event.to != toZone) continue

                        // Check source zone matches (if specified)
                        if (event.from != null && event.from != fromZone) continue

                        // Check filter against the entity being moved
                        if (!matchesZoneChangeFilter(state, entityId, container, event.filter, sourceControllerId)) continue

                        // Match found — redirect to new destination
                        return ZoneChangeRedirectResult(effect.newDestination)
                    }
                    is RedirectZoneChangeWithEffect -> {
                        // selfOnly: only applies when the entity being moved IS this permanent
                        if (effect.selfOnly && permanentId != entityId) continue

                        val event = effect.appliesTo
                        if (event !is com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent) continue

                        if (event.to != null && event.to != toZone) continue
                        if (event.from != null && event.from != fromZone) continue
                        if (!effect.selfOnly && !matchesZoneChangeFilter(state, entityId, container, event.filter, sourceControllerId)) continue

                        // Match found — redirect AND return additional effect
                        return ZoneChangeRedirectResult(
                            effect.newDestination,
                            effect.additionalEffect,
                            sourceControllerId
                        )
                    }
                    else -> continue
                }
            }
        }

        return ZoneChangeRedirectResult(toZone)
    }

    /**
     * Check if an entity matches a GameObjectFilter for zone change replacement effects.
     * Uses base state (not projected) since the entity may be leaving the battlefield.
     */
    private fun matchesZoneChangeFilter(
        state: GameState,
        entityId: EntityId,
        container: ComponentContainer,
        filter: GameObjectFilter,
        sourceControllerId: EntityId
    ): Boolean {
        if (filter == GameObjectFilter.Any) return true

        val cardComponent = container.get<CardComponent>() ?: return false

        // Check card predicates
        for (predicate in filter.cardPredicates) {
            val matches = when (predicate) {
                CardPredicate.IsCreature -> cardComponent.typeLine.isCreature
                CardPredicate.IsNontoken -> !container.has<TokenComponent>()
                CardPredicate.IsToken -> container.has<TokenComponent>()
                CardPredicate.IsLand -> cardComponent.typeLine.isLand
                CardPredicate.IsArtifact -> cardComponent.typeLine.isArtifact
                CardPredicate.IsEnchantment -> cardComponent.typeLine.isEnchantment
                CardPredicate.IsNonland -> !cardComponent.typeLine.isLand
                CardPredicate.IsNoncreature -> !cardComponent.typeLine.isCreature
                CardPredicate.IsNonenchantment -> !cardComponent.typeLine.isEnchantment
                CardPredicate.IsPermanent -> cardComponent.typeLine.isPermanent
                CardPredicate.IsLegendary -> cardComponent.typeLine.isLegendary
                CardPredicate.IsNonlegendary -> !cardComponent.typeLine.isLegendary
                else -> true // For unhandled predicates, don't filter out
            }
            if (!matches) return false
        }

        // Check controller/owner predicate
        val controllerPredicate = filter.controllerPredicate ?: return true
        return when (controllerPredicate) {
            ControllerPredicate.OwnedByOpponent -> {
                val ownerId = cardComponent.ownerId
                ownerId != null && ownerId != sourceControllerId
            }
            ControllerPredicate.OwnedByYou -> {
                cardComponent.ownerId == sourceControllerId
            }
            ControllerPredicate.ControlledByYou -> {
                val controllerId = container.get<ControllerComponent>()?.playerId
                controllerId == sourceControllerId
            }
            ControllerPredicate.ControlledByOpponent -> {
                val controllerId = container.get<ControllerComponent>()?.playerId
                controllerId != null && controllerId != sourceControllerId
            }
            else -> true
        }
    }

    /**
     * Apply the additional effect from a RedirectZoneChangeWithEffect replacement.
     * Supports TakeExtraTurnEffect (Ugin's Nexus) and AddCountersEffect (Darigaaz Reincarnated).
     *
     * @param entityId The entity that was redirected (for effects that target it, like adding counters)
     */
    fun applyReplacementAdditionalEffect(
        state: GameState,
        effect: com.wingedsheep.sdk.scripting.effects.Effect,
        controllerId: EntityId?,
        entityId: EntityId? = null
    ): GameState {
        if (effect is com.wingedsheep.sdk.scripting.effects.TakeExtraTurnEffect) {
            // Check if extra turns are prevented by any permanent on the battlefield
            if (ReplacementEffectUtils.isExtraTurnPrevented(state)) return state

            val cid = controllerId ?: return state
            val opponentId = state.getOpponent(cid) ?: return state
            return state.updateEntity(opponentId) { container ->
                container.with(SkipNextTurnComponent)
            }
        }
        if (effect is com.wingedsheep.sdk.scripting.effects.AddCountersEffect && entityId != null) {
            val counterType = try {
                com.wingedsheep.sdk.core.CounterType.valueOf(
                    effect.counterType.uppercase()
                        .replace(' ', '_')
                        .replace('+', 'P')
                        .replace('-', 'M')
                        .replace("/", "_")
                )
            } catch (_: IllegalArgumentException) {
                com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE
            }
            val current = state.getEntity(entityId)?.get<CountersComponent>() ?: CountersComponent()
            return state.updateEntity(entityId) { container ->
                container.with(current.withAdded(counterType, effect.count))
            }
        }
        return state
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
    fun applyRegenerationReplacement(state: GameState, entityId: EntityId): EffectResult {
        val entity = state.getEntity(entityId)
            ?: return EffectResult.success(state)

        val isAttacking = entity.has<AttackingComponent>()
        val isBlocking = entity.has<BlockingComponent>()

        // Tap, remove damage, and strip combat components from the regenerated creature
        var newState = state.updateEntity(entityId) { c ->
            c.with(TappedComponent)
                .without<DamageComponent>()
                .without<AttackingComponent>()
                .without<BlockingComponent>()
                .without<BlockedComponent>()
                .without<DamageAssignmentComponent>()
                .without<DamageAssignmentOrderComponent>()
                .without<AttackerOrderComponent>()
        }

        // Clean up cross-references in other creatures' combat components
        // (same logic as RemoveFromCombatExecutor)

        // If the regenerated creature was an attacker, remove it from blockers' BlockingComponent
        if (isAttacking) {
            for ((otherId, components) in newState.entities) {
                val blockingComponent = components.get<BlockingComponent>() ?: continue
                if (entityId in blockingComponent.blockedAttackerIds) {
                    val updatedIds = blockingComponent.blockedAttackerIds - entityId
                    newState = if (updatedIds.isEmpty()) {
                        newState.updateEntity(otherId) { c -> c.without<BlockingComponent>() }
                    } else {
                        newState.updateEntity(otherId) { c -> c.with(BlockingComponent(updatedIds)) }
                    }
                }
            }
        }

        // If the regenerated creature was a blocker, remove it from attackers' BlockedComponent.
        // Keep BlockedComponent even if empty — a blocked creature stays blocked per MTG rules
        // (it just deals no combat damage without trample).
        if (isBlocking) {
            val blockedAttackerIds = entity.get<BlockingComponent>()?.blockedAttackerIds ?: emptyList()
            for (attackerId in blockedAttackerIds) {
                val attackerEntity = newState.getEntity(attackerId) ?: continue
                val blockedComponent = attackerEntity.get<BlockedComponent>() ?: continue
                val updatedBlockerIds = blockedComponent.blockerIds - entityId
                newState = newState.updateEntity(attackerId) { c ->
                    c.with(BlockedComponent(updatedBlockerIds))
                }
            }
        }

        return EffectResult.success(newState)
    }

    /**
     * Move a permanent from battlefield to another zone, cleaning up permanent-only components.
     *
     * @param state The current game state
     * @param entityId The entity to move
     * @param targetZone The destination zone type
     * @return The execution result with updated state and events
     */
    fun movePermanentToZone(state: GameState, entityId: EntityId, targetZone: Zone): EffectResult {
        val container = state.getEntity(entityId)
            ?: return EffectResult.error(state, "Entity not found")

        container.get<CardComponent>()
            ?: return EffectResult.error(state, "Not a card")

        val result = ZoneTransitionService.moveToZone(state, entityId, targetZone)
        return EffectResult.success(result.state, result.events)
    }
}
