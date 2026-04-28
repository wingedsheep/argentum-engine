package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.LoyaltyChangedEvent
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.DamageDealtToCreaturesThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtDamageComponent
import com.wingedsheep.engine.state.components.battlefield.WasDealtDamageThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.player.DamageBonusComponent
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.scripting.NoncombatDamageBonus
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DamageCantBePrevented
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.ReplaceDamageWithCounters
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Utility functions for dealing damage, applying damage prevention/amplification/redirection,
 * tracking damage for triggers, and checking life gain prevention.
 */
object DamageUtils {

    private val predicateEvaluator = PredicateEvaluator()
    lateinit var cardRegistry: CardRegistry

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
    ): EffectResult {
        if (amount <= 0) return EffectResult.success(state)

        // Check for global "damage can't be prevented" effects (Sunspine Lynx, Leyline of Punishment)
        @Suppress("NAME_SHADOWING")
        val cantBePrevented = cantBePrevented || isDamagePreventionDisabled(state)

        // Check for damage redirection (Glarecaster, Zealous Inquisitor)
        val (redirectState, redirectTargetId, redirectAmount) = checkDamageRedirection(state, targetId, amount)
        if (redirectTargetId != null) {
            val redirectResult = dealDamageToTarget(redirectState, redirectTargetId, redirectAmount, sourceId, cantBePrevented)
            val remainingDamage = amount - redirectAmount
            return if (remainingDamage > 0) {
                // Partial redirection — deal remaining damage to original target
                val afterRedirect = redirectResult.state
                val remainingResult = dealDamageToTarget(afterRedirect, targetId, remainingDamage, sourceId, cantBePrevented)
                EffectResult.success(remainingResult.state, redirectResult.events + remainingResult.events)
            } else {
                redirectResult
            }
        }

        // Protection from color/subtype: damage from sources of the stated quality is prevented (Rule 702.16)
        if (!cantBePrevented && sourceId != null) {
            // Check if all damage from this source is prevented (Chain of Silence)
            if (isAllDamageFromSourcePrevented(state, sourceId)) {
                return EffectResult.success(state)
            }

            val projected = state.projectedState
            val sourceColors = projected.getColors(sourceId)
            for (colorName in sourceColors) {
                if (projected.hasKeyword(targetId, "PROTECTION_FROM_$colorName")) {
                    // Damage is prevented — return success with no state change
                    return EffectResult.success(state)
                }
            }
            val sourceSubtypes = projected.getSubtypes(sourceId)
            for (subtype in sourceSubtypes) {
                if (projected.hasKeyword(targetId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")) {
                    return EffectResult.success(state)
                }
            }

            // Protection from each opponent (Rule 702.16e)
            if (projected.hasKeyword(targetId, "PROTECTION_FROM_EACH_OPPONENT")) {
                val sourceController = projected.getController(sourceId)
                val targetController = projected.getController(targetId)
                    ?: state.getEntity(targetId)?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId
                if (sourceController != null && targetController != null && sourceController != targetController) {
                    return EffectResult.success(state)
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
            // Check for deflection shields (Deflecting Palm) — prevent + deal back to source's controller
            if (sourceId != null) {
                val deflectResult = checkDeflectDamageShield(newState, targetId, effectiveAmount, sourceId)
                if (deflectResult != null) return deflectResult
            }

            val (shieldState, reducedAmount) = applyDamagePreventionShields(newState, targetId, effectiveAmount, sourceId = sourceId)
            newState = shieldState
            effectiveAmount = reducedAmount
        }
        if (effectiveAmount <= 0) return EffectResult.success(newState)

        val events = mutableListOf<EngineGameEvent>()

        // Check if target is a player, planeswalker, or creature
        val lifeComponent = newState.getEntity(targetId)?.get<LifeTotalComponent>()
        val projected = newState.projectedState
        if (lifeComponent != null) {
            // It's a player - reduce life
            val newLife = lifeComponent.life - effectiveAmount
            newState = newState.updateEntity(targetId) { container ->
                container.with(LifeTotalComponent(newLife))
            }
            newState = trackDamageReceivedByPlayer(newState, targetId, effectiveAmount)
            events.add(LifeChangedEvent(targetId, lifeComponent.life, newLife, LifeChangeReason.DAMAGE))
        } else if (projected.isPlaneswalker(targetId)) {
            // It's a planeswalker - remove loyalty counters equal to damage dealt
            val counters = newState.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()
            val currentLoyalty = counters.getCount(CounterType.LOYALTY)
            newState = newState.updateEntity(targetId) { container ->
                container.with(counters.withRemoved(CounterType.LOYALTY, effectiveAmount))
            }
            val targetName = newState.getEntity(targetId)?.get<CardComponent>()?.name ?: "Planeswalker"
            events.add(LoyaltyChangedEvent(targetId, targetName, -(effectiveAmount.coerceAtMost(currentLoyalty))))
        } else {
            // It's a creature - mark damage (or place -1/-1 counters if source has wither)
            val hasWither = sourceId != null && projected.hasKeyword(sourceId, Keyword.WITHER)
            if (hasWither) {
                // Wither (CR 702.79): damage to creatures is dealt in the form of -1/-1 counters
                val counters = newState.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()
                newState = newState.updateEntity(targetId) { container ->
                    container.with(counters.withAdded(CounterType.MINUS_ONE_MINUS_ONE, effectiveAmount))
                }
                events.add(CountersAddedEvent(targetId, CounterType.MINUS_ONE_MINUS_ONE.name, effectiveAmount,
                    newState.getEntity(targetId)?.get<CardComponent>()?.name ?: "Creature"))
            } else {
                val existingDamage = newState.getEntity(targetId)?.get<DamageComponent>()
                val currentDamage = existingDamage?.amount ?: 0
                val hasDeathtouch = sourceId != null && projected.hasKeyword(sourceId, Keyword.DEATHTOUCH)
                newState = newState.updateEntity(targetId) { container ->
                    container.with(DamageComponent(
                        amount = currentDamage + effectiveAmount,
                        deathtouchDamageReceived = hasDeathtouch || (existingDamage?.deathtouchDamageReceived == true)
                    ))
                }
            }
            // Mark creature as having been dealt damage this turn
            newState = newState.updateEntity(targetId) { container ->
                container.with(WasDealtDamageThisTurnComponent)
            }
            // Track damage source for "creature dealt damage by this dies" triggers
            if (sourceId != null) {
                newState = trackDamageDealtToCreature(newState, sourceId, targetId)
            }
        }

        // Mark source as having dealt damage (lifetime tracking)
        if (sourceId != null && sourceId in newState.getBattlefield()) {
            newState = newState.updateEntity(sourceId) { container ->
                container.with(HasDealtDamageComponent)
            }
        }

        val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val targetContainer = newState.getEntity(targetId)
        val targetName = targetContainer?.get<CardComponent>()?.name
        val targetIsPlayer = targetContainer?.get<LifeTotalComponent>() != null
        val targetIsFaceDown = targetContainer?.has<FaceDownComponent>() == true
        events.add(DamageDealtEvent(sourceId, targetId, effectiveAmount, false, sourceName = sourceName, targetName = targetName, targetIsPlayer = targetIsPlayer, targetWasFaceDown = targetIsFaceDown))

        // Lifelink: if the source has lifelink, its controller gains life equal to the damage dealt (Rule 702.15)
        if (sourceId != null) {
            val projected = newState.projectedState
            if (projected.hasKeyword(sourceId, Keyword.LIFELINK.name)) {
                val controllerId = projected.getController(sourceId)
                    ?: newState.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
                if (controllerId != null && !isLifeGainPrevented(newState, controllerId)) {
                    val currentLife = newState.getEntity(controllerId)?.get<LifeTotalComponent>()?.life
                    if (currentLife != null) {
                        val newLife = currentLife + effectiveAmount
                        newState = newState.updateEntity(controllerId) { container ->
                            container.with(LifeTotalComponent(newLife))
                        }
                        events.add(LifeChangedEvent(controllerId, currentLife, newLife, LifeChangeReason.LIFE_GAIN))
                    }
                }
            }
        }

        return EffectResult.success(newState, events)
    }

    /**
     * Track that [playerId] received [amount] damage this turn.
     * Updates the DamageReceivedThisTurnComponent on the player entity.
     * Also marks the player as having lost life this turn (LifeLostThisTurnComponent).
     * Used for Final Punishment: "Target player loses life equal to the damage
     * already dealt to that player this turn."
     */
    fun trackDamageReceivedByPlayer(state: GameState, playerId: EntityId, amount: Int): GameState {
        if (amount <= 0) return state
        return state.updateEntity(playerId) { container ->
            val existing = container.get<com.wingedsheep.engine.state.components.player.DamageReceivedThisTurnComponent>()
                ?: com.wingedsheep.engine.state.components.player.DamageReceivedThisTurnComponent()
            container.with(com.wingedsheep.engine.state.components.player.DamageReceivedThisTurnComponent(existing.amount + amount))
                .with(com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent)
        }
    }

    /**
     * Mark that [playerId] gained life this turn.
     * Sets the LifeGainedThisTurnComponent on the player entity.
     * Used for conditions like "if you gained life this turn" (Lunar Convocation).
     */
    fun markLifeGainedThisTurn(state: GameState, playerId: EntityId): GameState {
        return state.updateEntity(playerId) { container ->
            container.with(com.wingedsheep.engine.state.components.player.LifeGainedThisTurnComponent)
        }
    }

    /**
     * Mark that [playerId] lost life this turn (non-damage life loss, e.g., from LoseLife effects or payments).
     * Sets the LifeLostThisTurnComponent on the player entity.
     * Used for conditions like "if an opponent lost life this turn" (Hired Claw).
     */
    fun markLifeLostThisTurn(state: GameState, playerId: EntityId): GameState {
        return state.updateEntity(playerId) { container ->
            container.with(com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent)
        }
    }

    /**
     * Mark that [placerId] put one or more counters on the creature [targetId] this turn.
     * Only marks when [targetId] is a creature in the projected state.
     * Sets the PutCounterOnCreatureThisTurnComponent on the placing player's entity.
     * Used for conditions like "if you put a counter on a creature this turn" (Lasting Tarfire).
     */
    fun markCounterPlacedOnCreature(state: GameState, placerId: EntityId, targetId: EntityId): GameState {
        if (!state.projectedState.isCreature(targetId)) return state
        return state.updateEntity(placerId) { container ->
            container.with(com.wingedsheep.engine.state.components.player.PutCounterOnCreatureThisTurnComponent)
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
     * Check if life gain is prevented for a player by any PreventLifeGain replacement effect
     * on the battlefield (e.g., Sulfuric Vortex, Erebos).
     */
    fun isLifeGainPrevented(state: GameState, playerId: EntityId): Boolean {
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is PreventLifeGain) continue

                val lifeGainEvent = effect.appliesTo
                if (lifeGainEvent !is com.wingedsheep.sdk.scripting.GameEvent.LifeGainEvent) continue

                when (lifeGainEvent.player) {
                    Player.Each -> return true
                    Player.You -> {
                        val sourceControllerId = container.get<ControllerComponent>()?.playerId
                        if (playerId == sourceControllerId) return true
                    }
                    Player.Opponent -> {
                        val sourceControllerId = container.get<ControllerComponent>()?.playerId
                        if (playerId != sourceControllerId) return true
                    }
                    else -> {}
                }
            }
        }
        return false
    }

    /**
     * Check if damage prevention is globally disabled by any DamageCantBePrevented replacement effect
     * on the battlefield (e.g., Sunspine Lynx, Leyline of Punishment).
     */
    fun isDamagePreventionDisabled(state: GameState): Boolean {
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect is DamageCantBePrevented) return true
            }
        }
        return false
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

        if (updatedEffects.any {
                it.effect.modification is SerializableModification.PreventAllDamageTo &&
                    targetId in it.effect.affectedEntities
            }) {
            return state to 0
        }

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
            val projected = state.projectedState
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
     * Check for deflect damage shields (Deflecting Palm).
     *
     * Scans floating effects for DeflectNextDamageFromSource matching the damage source.
     * If found, consumes the shield, prevents all the damage, and deals that much damage
     * to the source's controller (with the deflecting card as the damage source).
     *
     * @param state The current game state
     * @param targetId The entity about to receive damage (must be the shield's affected entity)
     * @param damageAmount The amount of damage about to be dealt
     * @param sourceId The entity dealing the damage
     * @return ExecutionResult if deflection occurred, null otherwise
     */
    fun checkDeflectDamageShield(
        state: GameState,
        targetId: EntityId,
        damageAmount: Int,
        sourceId: EntityId
    ): EffectResult? {
        val shieldIndex = state.floatingEffects.indexOfFirst { effect ->
            val mod = effect.effect.modification
            mod is SerializableModification.DeflectNextDamageFromSource &&
                mod.damageSourceId == sourceId &&
                targetId in effect.effect.affectedEntities
        }
        if (shieldIndex == -1) return null

        val shield = state.floatingEffects[shieldIndex]
        val mod = shield.effect.modification as SerializableModification.DeflectNextDamageFromSource

        // Consume the shield
        val updatedEffects = state.floatingEffects.toMutableList()
        updatedEffects.removeAt(shieldIndex)
        val newState = state.copy(floatingEffects = updatedEffects)

        // Find the source's controller to deal reflected damage to
        val sourceController = newState.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
        if (sourceController == null) {
            // Source has no controller (e.g., it left the game) — damage is still prevented
            return EffectResult.success(newState)
        }

        // Deal the reflected damage to the source's controller, sourced from the deflecting card
        return dealDamageToTarget(newState, sourceController, damageAmount, mod.deflectSourceId)
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
        val projected = state.projectedState

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
                    is RecipientFilter.EnchantedCreature, is RecipientFilter.EquippedCreature -> {
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
        sourceId: EntityId?,
        isCombatDamage: Boolean = false
    ): Int {
        if (amount <= 0) return 0

        var amplifiedAmount = amount
        val projected = state.projectedState

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

        // Check for ModifyDamageAmount replacement effects (Valley Flamecaller)
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = container.get<ControllerComponent>()?.playerId ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is ModifyDamageAmount) continue

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

                amplifiedAmount += effect.modifier
            }
        }

        // Check player-level damage bonus components (e.g., The Flame of Keld Chapter III)
        if (sourceId != null) {
            for (playerId in state.turnOrder) {
                val playerContainer = state.getEntity(playerId) ?: continue
                val damageBonusComponent = playerContainer.get<DamageBonusComponent>() ?: continue

                // Check that the source is controlled by this player
                val sourceController = state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
                if (sourceController != playerId) continue

                // Check if the source matches the source filter
                val sourceMatches = when (val filter = damageBonusComponent.sourceFilter) {
                    is SourceFilter.Any -> true
                    is SourceFilter.HasColor -> {
                        // Check projected state first (for battlefield permanents), fall back to base colors
                        // (for spells on the stack or other non-battlefield entities)
                        hasColorForSource(state, projected, sourceId, filter.color)
                    }
                    is SourceFilter.Matching -> {
                        val context = PredicateContext(controllerId = playerId)
                        predicateEvaluator.matchesWithProjection(state, projected, sourceId, filter.filter, context)
                    }
                    else -> false
                }
                if (!sourceMatches) continue

                amplifiedAmount += damageBonusComponent.bonusAmount
            }
        }

        // Check battlefield permanents for NoncombatDamageBonus static abilities (Artist's Talent Level 3)
        if (sourceId != null && !isCombatDamage) {
            val sourceController = projected.getController(sourceId)
                ?: state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
            if (sourceController != null) {
                for (entityId in state.controlledBattlefield(sourceController)) {
                    val container = state.getEntity(entityId) ?: continue
                    val card = container.get<CardComponent>() ?: continue
                    val permanentDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                    val classLevel = container.get<ClassLevelComponent>()?.currentLevel

                    for (ability in permanentDef.script.effectiveStaticAbilities(classLevel)) {
                        if (ability !is NoncombatDamageBonus) continue

                        // Check target is an opponent or a permanent an opponent controls
                        val targetIsOpponent = targetId in state.turnOrder && targetId != sourceController
                        val targetController = state.getEntity(targetId)?.get<ControllerComponent>()?.playerId
                        val targetIsOpponentPermanent = targetController != null && targetController != sourceController

                        if (targetIsOpponent || targetIsOpponentPermanent) {
                            amplifiedAmount += ability.bonusAmount
                        }
                    }
                }
            }
        }

        return amplifiedAmount
    }

    /**
     * Check if a source entity has a specific color — checks projected state first,
     * then falls back to base CardComponent colors (for spells on the stack).
     */
    private fun hasColorForSource(
        state: GameState,
        projected: com.wingedsheep.engine.mechanics.layers.ProjectedState,
        sourceId: EntityId,
        color: com.wingedsheep.sdk.core.Color
    ): Boolean {
        if (projected.hasColor(sourceId, color)) return true
        val sourceEntity = state.getEntity(sourceId) ?: return false
        val card = sourceEntity.components[CardComponent::class.qualifiedName] as? CardComponent ?: return false
        return card.colors.contains(color)
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
    ): EffectResult? {
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
                    // Delegate zone movement to ZoneTransitionService for full cleanup
                    val transitionResult = ZoneTransitionService.moveToZone(
                        newState, entityId, Zone.GRAVEYARD
                    )
                    newState = transitionResult.state
                    events.add(
                        PermanentsSacrificedEvent(
                            sourceControllerId,
                            listOf(entityId),
                            listOf(entityName)
                        )
                    )
                    events.addAll(transitionResult.events)
                }

                return EffectResult.success(newState, events)
            }
        }

        return null
    }
}
