package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.EffectHandler
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.mechanics.text.SubtypeReplacer
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Effect

/**
 * Manages the stack: casting spells, activating abilities, and resolution.
 *
 * Handles:
 * - Putting spells on the stack
 * - Putting triggered abilities on the stack
 * - Putting activated abilities on the stack
 * - Resolving the top item
 * - Target validation on resolution
 * - Countering spells
 */
class StackResolver(
    private val effectHandler: EffectHandler = EffectHandler(),
    private val cardRegistry: CardRegistry? = null,
    private val staticAbilityHandler: StaticAbilityHandler = StaticAbilityHandler(cardRegistry)
) {

    // =========================================================================
    // Casting Spells
    // =========================================================================

    /**
     * Put a spell on the stack.
     *
     * @param castFaceDown If true, cast as a face-down 2/2 creature (morph). The spell
     *                     will resolve as a face-down creature with FaceDownComponent
     *                     and MorphDataComponent.
     * @param damageDistribution Pre-chosen damage distribution for DividedDamageEffect spells
     */
    fun castSpell(
        state: GameState,
        cardId: EntityId,
        casterId: EntityId,
        targets: List<ChosenTarget> = emptyList(),
        xValue: Int? = null,
        sacrificedPermanents: List<EntityId> = emptyList(),
        castFaceDown: Boolean = false,
        damageDistribution: Map<EntityId, Int>? = null
    ): ExecutionResult {
        val container = state.getEntity(cardId)
            ?: return ExecutionResult.error(state, "Card not found: $cardId")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card: $cardId")

        // Remove from current zone (typically hand)
        var newState = removeFromCurrentZone(state, cardId, casterId)

        // Add spell components
        newState = newState.updateEntity(cardId) { c ->
            var updated = c.with(SpellOnStackComponent(
                casterId = casterId,
                xValue = xValue,
                sacrificedPermanents = sacrificedPermanents,
                castFaceDown = castFaceDown,
                damageDistribution = damageDistribution
            ))
            if (targets.isNotEmpty()) {
                updated = updated.with(TargetsComponent(targets))
            }
            // Add morph data for creatures with morph (needed for face-down casting and
            // for effects like Backslide that target "creature with a morph ability")
            val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId)
            val morphAbility = cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Morph>()?.firstOrNull()
            if (morphAbility != null) {
                updated = updated.with(MorphDataComponent(
                    morphCost = morphAbility.cost,
                    originalCardDefinitionId = cardComponent.cardDefinitionId
                ))
            }
            updated
        }

        // Push to stack
        newState = newState.pushToStack(cardId)

        // For face-down creatures, use a generic name in the event
        val eventName = if (castFaceDown) "Face-down creature" else cardComponent.name

        return ExecutionResult.success(
            newState.tick(),
            listOf(SpellCastEvent(cardId, eventName, casterId))
        )
    }

    /**
     * Put a triggered ability on the stack.
     */
    fun putTriggeredAbility(
        state: GameState,
        ability: TriggeredAbilityOnStackComponent,
        targets: List<ChosenTarget> = emptyList()
    ): ExecutionResult {
        // Create a new entity for the ability on the stack
        val abilityId = EntityId.generate()

        var container = ComponentContainer.of(ability)
        if (targets.isNotEmpty()) {
            container = container.with(TargetsComponent(targets))
        }

        var newState = state.withEntity(abilityId, container)
        newState = newState.pushToStack(abilityId)

        return ExecutionResult.success(
            newState.tick(),
            listOf(
                AbilityTriggeredEvent(
                    ability.sourceId,
                    ability.sourceName,
                    ability.controllerId,
                    ability.description
                )
            )
        )
    }

    /**
     * Put an activated ability on the stack.
     */
    fun putActivatedAbility(
        state: GameState,
        ability: ActivatedAbilityOnStackComponent,
        targets: List<ChosenTarget> = emptyList()
    ): ExecutionResult {
        val abilityId = EntityId.generate()

        var container = ComponentContainer.of(ability)
        if (targets.isNotEmpty()) {
            container = container.with(TargetsComponent(targets))
        }

        var newState = state.withEntity(abilityId, container)
        newState = newState.pushToStack(abilityId)

        return ExecutionResult.success(
            newState.tick(),
            listOf(
                AbilityActivatedEvent(
                    ability.sourceId,
                    ability.sourceName,
                    ability.controllerId
                )
            )
        )
    }

    // =========================================================================
    // Resolution
    // =========================================================================

    /**
     * Resolve the top item on the stack.
     */
    fun resolveTop(state: GameState): ExecutionResult {
        val topId = state.getTopOfStack()
            ?: return ExecutionResult.error(state, "Stack is empty")

        val container = state.getEntity(topId)
            ?: return ExecutionResult.error(state, "Stack item not found: $topId")

        // Pop from stack
        val (_, poppedState) = state.popFromStack()

        // Determine what type of item this is
        return when {
            container.has<SpellOnStackComponent>() ->
                resolveSpell(poppedState, topId, container)

            container.has<TriggeredAbilityOnStackComponent>() ->
                resolveTriggeredAbility(poppedState, topId, container)

            container.has<ActivatedAbilityOnStackComponent>() ->
                resolveActivatedAbility(poppedState, topId, container)

            else ->
                ExecutionResult.error(state, "Unknown stack item type")
        }
    }

    /**
     * Resolve a spell.
     */
    private fun resolveSpell(
        state: GameState,
        spellId: EntityId,
        container: ComponentContainer
    ): ExecutionResult {
        val cardComponent = container.get<CardComponent>()
        val spellComponent = container.get<SpellOnStackComponent>()!!
        val targetsComponent = container.get<TargetsComponent>()

        // Validate targets if spell has any
        if (targetsComponent != null && targetsComponent.targets.isNotEmpty()) {
            val validTargets = validateTargets(state, targetsComponent.targets)
            if (validTargets.isEmpty()) {
                // All targets invalid - spell fizzles
                return fizzleSpell(state, spellId, cardComponent, spellComponent)
            }
        }

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Check if permanent or non-permanent
        val isPermanent = cardComponent?.typeLine?.isPermanent ?: false

        if (isPermanent) {
            // Put permanent on battlefield
            newState = resolvePermanentSpell(newState, spellId, spellComponent, cardComponent)
            events.add(ResolvedEvent(spellId, cardComponent?.name ?: "Unknown"))
            events.add(
                ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    null, // Was on stack
                    Zone.BATTLEFIELD,
                    cardComponent?.ownerId ?: spellComponent.casterId
                )
            )
        } else {
            // Execute effects and put in graveyard
            val effectResult = resolveNonPermanentSpell(
                newState, spellId, spellComponent, cardComponent,
                targetsComponent?.targets ?: emptyList()
            )
            newState = effectResult.newState
            events.addAll(effectResult.events)
            events.add(ResolvedEvent(spellId, cardComponent?.name ?: "Unknown"))
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Resolve a permanent spell - put it on the battlefield.
     */
    private fun resolvePermanentSpell(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?
    ): GameState {
        val controllerId = spellComponent.casterId

        // For Auras: get the target before removing TargetsComponent
        val auraTargetId = if (cardComponent?.isAura == true) {
            state.getEntity(spellId)?.get<TargetsComponent>()?.targets?.firstOrNull()?.let { target ->
                when (target) {
                    is ChosenTarget.Permanent -> target.entityId
                    else -> null
                }
            }
        } else null

        // Update entity: remove spell components, add permanent components
        var newState = state.updateEntity(spellId) { c ->
            var updated = c.without<SpellOnStackComponent>()
                .without<TargetsComponent>()
                .with(ControllerComponent(controllerId))

            // If cast face-down (morph), add FaceDownComponent
            // MorphDataComponent was already added when the spell was cast
            if (spellComponent.castFaceDown) {
                updated = updated.with(FaceDownComponent)
            }

            // Creatures enter with summoning sickness (including face-down creatures)
            if (cardComponent?.typeLine?.isCreature == true || spellComponent.castFaceDown) {
                updated = updated.with(SummoningSicknessComponent)
            }

            // Add continuous effects from static abilities (but not for face-down creatures)
            if (!spellComponent.castFaceDown) {
                updated = staticAbilityHandler.addContinuousEffectComponent(updated)
            }

            // Aura attachment: add AttachedToComponent pointing to the target
            if (auraTargetId != null) {
                updated = updated.with(
                    com.wingedsheep.engine.state.components.battlefield.AttachedToComponent(auraTargetId)
                )
            }

            updated
        }

        // Add to battlefield
        val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, spellId)

        return newState
    }

    /**
     * Resolve a non-permanent spell - execute effects, put in graveyard.
     */
    private fun resolveNonPermanentSpell(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?,
        targets: List<ChosenTarget>
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        // Execute the spell effect if present, applying text replacement if the spell
        // was modified by a text-changing effect (e.g., Artificial Evolution)
        val rawSpellEffect = cardComponent?.spellEffect
        val textReplacement = state.getEntity(spellId)?.get<TextReplacementComponent>()
        val spellEffect = if (rawSpellEffect != null && textReplacement != null) {
            SubtypeReplacer.replaceEffect(rawSpellEffect, textReplacement)
        } else {
            rawSpellEffect
        }
        if (spellEffect != null) {
            val context = EffectContext(
                sourceId = spellId,
                controllerId = spellComponent.casterId,
                opponentId = newState.getOpponent(spellComponent.casterId),
                targets = targets,
                xValue = spellComponent.xValue,
                sacrificedPermanents = spellComponent.sacrificedPermanents,
                damageDistribution = spellComponent.damageDistribution
            )

            val effectResult = effectHandler.execute(newState, spellEffect, context)

            // If effect is paused awaiting a decision, we still need to move the spell
            // to graveyard (it has already resolved from the stack). The decision only
            // determines how the effect completes.
            if (effectResult.isPaused) {
                val ownerId = cardComponent?.ownerId ?: spellComponent.casterId
                val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

                // Move spell to graveyard even though effect is paused
                var pausedState = effectResult.state.updateEntity(spellId) { c ->
                    c.without<SpellOnStackComponent>().without<TargetsComponent>()
                }
                pausedState = pausedState.addToZone(graveyardZone, spellId)

                // Include the zone change event along with effect events
                val allEvents = events + effectResult.events + ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    null,
                    Zone.GRAVEYARD,
                    ownerId
                )

                return ExecutionResult.paused(
                    pausedState,
                    effectResult.pendingDecision!!,
                    allEvents
                )
            }

            // Always apply state changes from effect execution, even on partial
            // failure. Per MTG rules, when a spell resolves, you do as much as
            // possible. Partial state changes (e.g., first target destroyed but
            // second target missing) should be preserved.
            newState = effectResult.newState
            events.addAll(effectResult.events)
        }

        // Move to graveyard
        val ownerId = cardComponent?.ownerId ?: spellComponent.casterId
        val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

        newState = newState.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>().without<TargetsComponent>()
        }
        newState = newState.addToZone(graveyardZone, spellId)

        events.add(
            ZoneChangeEvent(
                spellId,
                cardComponent?.name ?: "Unknown",
                null,
                Zone.GRAVEYARD,
                ownerId
            )
        )

        return ExecutionResult.success(newState, events)
    }

    /**
     * Spell fizzles because all targets are invalid.
     */
    private fun fizzleSpell(
        state: GameState,
        spellId: EntityId,
        cardComponent: CardComponent?,
        spellComponent: SpellOnStackComponent
    ): ExecutionResult {
        val ownerId = cardComponent?.ownerId ?: spellComponent.casterId
        val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

        var newState = state.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>().without<TargetsComponent>()
        }
        newState = newState.addToZone(graveyardZone, spellId)

        return ExecutionResult.success(
            newState,
            listOf(
                SpellFizzledEvent(spellId, cardComponent?.name ?: "Unknown", "All targets are invalid"),
                ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    null,
                    Zone.GRAVEYARD,
                    ownerId
                )
            )
        )
    }

    /**
     * Resolve a triggered ability.
     */
    private fun resolveTriggeredAbility(
        state: GameState,
        abilityId: EntityId,
        container: ComponentContainer
    ): ExecutionResult {
        val abilityComponent = container.get<TriggeredAbilityOnStackComponent>()!!
        val targetsComponent = container.get<TargetsComponent>()

        // Validate targets
        if (targetsComponent != null && targetsComponent.targets.isNotEmpty()) {
            val validTargets = validateTargets(state, targetsComponent.targets)
            if (validTargets.isEmpty()) {
                // Fizzle - remove ability entity
                val newState = state.removeEntity(abilityId)
                return ExecutionResult.success(
                    newState,
                    listOf(
                        AbilityFizzledEvent(
                            abilityComponent.sourceId,
                            abilityComponent.description,
                            "All targets are invalid"
                        )
                    )
                )
            }
        }

        // Execute the effect
        val context = EffectContext(
            sourceId = abilityComponent.sourceId,
            controllerId = abilityComponent.controllerId,
            opponentId = state.getOpponent(abilityComponent.controllerId),
            targets = targetsComponent?.targets ?: emptyList(),
            triggerDamageAmount = abilityComponent.triggerDamageAmount,
            triggeringEntityId = abilityComponent.triggeringEntityId
        )

        val effectResult = effectHandler.execute(state, abilityComponent.effect, context)

        // If effect is paused awaiting a decision, return paused state
        // The ability entity stays removed (it's off the stack), but the decision must resolve
        if (effectResult.isPaused) {
            val pausedState = effectResult.state.removeEntity(abilityId)
            return ExecutionResult.paused(
                pausedState,
                effectResult.pendingDecision!!,
                effectResult.events
            )
        }

        var newState = effectResult.newState

        // Remove the ability entity
        newState = newState.removeEntity(abilityId)

        return ExecutionResult.success(
            newState,
            effectResult.events + AbilityResolvedEvent(
                abilityComponent.sourceId,
                abilityComponent.description
            )
        )
    }

    /**
     * Resolve an activated ability.
     */
    private fun resolveActivatedAbility(
        state: GameState,
        abilityId: EntityId,
        container: ComponentContainer
    ): ExecutionResult {
        val abilityComponent = container.get<ActivatedAbilityOnStackComponent>()!!
        val targetsComponent = container.get<TargetsComponent>()

        // Validate targets
        if (targetsComponent != null && targetsComponent.targets.isNotEmpty()) {
            val validTargets = validateTargets(state, targetsComponent.targets)
            if (validTargets.isEmpty()) {
                val newState = state.removeEntity(abilityId)
                return ExecutionResult.success(
                    newState,
                    listOf(
                        AbilityFizzledEvent(
                            abilityComponent.sourceId,
                            abilityComponent.sourceName,
                            "All targets are invalid"
                        )
                    )
                )
            }
        }

        // Execute the effect
        val context = EffectContext(
            sourceId = abilityComponent.sourceId,
            controllerId = abilityComponent.controllerId,
            opponentId = state.getOpponent(abilityComponent.controllerId),
            targets = targetsComponent?.targets ?: emptyList()
        )

        val effectResult = effectHandler.execute(state, abilityComponent.effect, context)

        // If effect is paused awaiting a decision, return paused state
        // The ability entity stays removed (it's off the stack), but the decision must resolve
        if (effectResult.isPaused) {
            val pausedState = effectResult.state.removeEntity(abilityId)
            return ExecutionResult.paused(
                pausedState,
                effectResult.pendingDecision!!,
                effectResult.events
            )
        }

        var newState = effectResult.newState

        // Remove the ability entity
        newState = newState.removeEntity(abilityId)

        return ExecutionResult.success(
            newState,
            effectResult.events + AbilityResolvedEvent(
                abilityComponent.sourceId,
                abilityComponent.sourceName
            )
        )
    }

    // =========================================================================
    // Countering
    // =========================================================================

    /**
     * Counter a spell on the stack.
     */
    fun counterSpell(state: GameState, spellId: EntityId): ExecutionResult {
        if (spellId !in state.stack) {
            return ExecutionResult.error(state, "Spell not on stack: $spellId")
        }

        val container = state.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Spell not found: $spellId")

        val cardComponent = container.get<CardComponent>()
        val spellComponent = container.get<SpellOnStackComponent>()
        val ownerId = cardComponent?.ownerId
            ?: spellComponent?.casterId
            ?: return ExecutionResult.error(state, "Cannot determine spell owner")

        // Remove from stack
        var newState = state.removeFromStack(spellId)

        // Put in graveyard
        val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)
        newState = newState.addToZone(graveyardZone, spellId)

        // Remove stack components
        newState = newState.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>().without<TargetsComponent>()
        }

        return ExecutionResult.success(
            newState,
            listOf(
                SpellCounteredEvent(spellId, cardComponent?.name ?: "Unknown"),
                ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    null,
                    Zone.GRAVEYARD,
                    ownerId
                )
            )
        )
    }

    // =========================================================================
    // Target Validation
    // =========================================================================

    /**
     * Validate targets and return only valid ones.
     */
    private fun validateTargets(
        state: GameState,
        targets: List<ChosenTarget>
    ): List<ChosenTarget> {
        return targets.filter { target ->
            when (target) {
                is ChosenTarget.Player -> {
                    // Player is valid if they exist and haven't lost
                    state.hasEntity(target.playerId)
                }

                is ChosenTarget.Permanent -> {
                    // Permanent is valid if still on battlefield
                    target.entityId in state.getBattlefield()
                }

                is ChosenTarget.Card -> {
                    // Card is valid if in expected zone
                    val zoneKey = ZoneKey(target.ownerId, target.zone)
                    target.cardId in state.getZone(zoneKey)
                }

                is ChosenTarget.Spell -> {
                    // Spell is valid if still on stack
                    target.spellEntityId in state.stack
                }
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Remove a card from its current zone (for casting).
     */
    private fun removeFromCurrentZone(
        state: GameState,
        cardId: EntityId,
        playerId: EntityId
    ): GameState {
        // Try removing from hand first
        val handZone = ZoneKey(playerId, Zone.HAND)
        if (cardId in state.getZone(handZone)) {
            return state.removeFromZone(handZone, cardId)
        }

        // Also check graveyard (for flashback etc.)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        if (cardId in state.getZone(graveyardZone)) {
            return state.removeFromZone(graveyardZone, cardId)
        }

        // Check exile
        val exileZone = ZoneKey(playerId, Zone.EXILE)
        if (cardId in state.getZone(exileZone)) {
            return state.removeFromZone(exileZone, cardId)
        }

        return state
    }
}
