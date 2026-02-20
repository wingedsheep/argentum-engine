package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.EffectHandler
import com.wingedsheep.engine.mechanics.layers.StateProjector
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
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EntersWithCreatureTypeChoice
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*

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
    internal val cardRegistry: CardRegistry? = null,
    private val staticAbilityHandler: StaticAbilityHandler = StaticAbilityHandler(cardRegistry),
    private val stateProjector: StateProjector = StateProjector(),
    private val predicateEvaluator: PredicateEvaluator = PredicateEvaluator()
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
        sacrificedPermanentSubtypes: Map<EntityId, Set<String>> = emptyMap(),
        castFaceDown: Boolean = false,
        damageDistribution: Map<EntityId, Int>? = null,
        targetRequirements: List<TargetRequirement> = emptyList(),
        chosenCreatureType: String? = null
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
                sacrificedPermanentSubtypes = sacrificedPermanentSubtypes,
                castFaceDown = castFaceDown,
                damageDistribution = damageDistribution,
                chosenCreatureType = chosenCreatureType
            ))
            if (targets.isNotEmpty()) {
                updated = updated.with(TargetsComponent(targets, targetRequirements))
            }
            // Add morph data for creatures with morph (needed for face-down casting and
            // for effects like Backslide that target "creature with a morph ability")
            val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId)
            val morphAbility = cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Morph>()?.firstOrNull()
            if (morphAbility != null) {
                updated = updated.with(MorphDataComponent(
                    morphCost = morphAbility.morphCost,
                    originalCardDefinitionId = cardComponent.cardDefinitionId
                ))
            }
            updated
        }

        // Push to stack and reset priority passes (new stack item requires fresh round of passes)
        newState = newState.pushToStack(cardId)
            .copy(priorityPassedBy = emptySet())

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
        targets: List<ChosenTarget> = emptyList(),
        targetRequirements: List<TargetRequirement> = emptyList()
    ): ExecutionResult {
        // Create a new entity for the ability on the stack
        val abilityId = EntityId.generate()

        var container = ComponentContainer.of(ability)
        if (targets.isNotEmpty()) {
            container = container.with(TargetsComponent(targets, targetRequirements))
        }

        var newState = state.withEntity(abilityId, container)
        newState = newState.pushToStack(abilityId)
            .copy(priorityPassedBy = emptySet())

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
        targets: List<ChosenTarget> = emptyList(),
        targetRequirements: List<TargetRequirement> = emptyList()
    ): ExecutionResult {
        val abilityId = EntityId.generate()

        var container = ComponentContainer.of(ability)
        if (targets.isNotEmpty()) {
            container = container.with(TargetsComponent(targets, targetRequirements))
        }

        var newState = state.withEntity(abilityId, container)
        newState = newState.pushToStack(abilityId)
            .copy(priorityPassedBy = emptySet())

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

        // Validate targets if spell has any (including protection check - Rule 702.16)
        val sourceColors = cardComponent?.colors ?: emptySet()
        val sourceSubtypes = cardComponent?.typeLine?.subtypes?.map { it.value }?.toSet() ?: emptySet()
        val resolvedTargets = if (targetsComponent != null && targetsComponent.targets.isNotEmpty()) {
            val validTargets = validateTargets(
                state, targetsComponent.targets, sourceColors, sourceSubtypes,
                spellComponent.casterId, targetsComponent.targetRequirements,
                sourceId = spellId
            )
            if (validTargets.isEmpty()) {
                // All targets invalid - spell fizzles
                return fizzleSpell(state, spellId, cardComponent, spellComponent)
            }
            validTargets
        } else {
            targetsComponent?.targets ?: emptyList()
        }

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Check if permanent or non-permanent
        val isPermanent = cardComponent?.typeLine?.isPermanent ?: false

        if (isPermanent) {
            // Put permanent on battlefield
            val permanentResult = resolvePermanentSpell(newState, spellId, spellComponent, cardComponent)
            if (permanentResult.isPaused) {
                return ExecutionResult.paused(
                    permanentResult.state,
                    permanentResult.pendingDecision!!,
                    events + permanentResult.events
                )
            }
            newState = permanentResult.state
            events.addAll(permanentResult.events)
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
                resolvedTargets
            )
            if (effectResult.isPaused) {
                // Effect paused for a decision (e.g., draw replacement prompt).
                // resolveNonPermanentSpell already moved spell to graveyard.
                val allEvents = events + effectResult.events +
                    ResolvedEvent(spellId, cardComponent?.name ?: "Unknown")
                return ExecutionResult.paused(
                    effectResult.state,
                    effectResult.pendingDecision!!,
                    allEvents
                )
            }
            newState = effectResult.newState
            events.addAll(effectResult.events)
            events.add(ResolvedEvent(spellId, cardComponent?.name ?: "Unknown"))
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Resolve a permanent spell - put it on the battlefield.
     * May pause for player input (e.g., Clone choosing a creature to copy).
     */
    private fun resolvePermanentSpell(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?
    ): ExecutionResult {
        val controllerId = spellComponent.casterId
        val ownerId = cardComponent?.ownerId ?: controllerId

        // Check for EntersAsCopy replacement effect before entering the battlefield
        val cardDef = cardComponent?.cardDefinitionId?.let { cardRegistry?.getCard(it) }
        if (cardDef != null && !spellComponent.castFaceDown) {
            val entersAsCopy = cardDef.script.replacementEffects.filterIsInstance<EntersAsCopy>().firstOrNull()
            if (entersAsCopy != null) {
                // Find all creatures on the battlefield
                val creatures = state.getBattlefield().filter { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>()?.isCreature == true
                }

                if (creatures.isNotEmpty()) {
                    // Present the selection decision
                    val decisionId = "clone-enters-${spellId.value}"
                    val decision = SelectCardsDecision(
                        id = decisionId,
                        playerId = controllerId,
                        prompt = if (entersAsCopy.optional) {
                            "You may choose a creature to copy"
                        } else {
                            "Choose a creature to copy"
                        },
                        context = DecisionContext(
                            sourceId = spellId,
                            sourceName = cardComponent?.name,
                            phase = DecisionPhase.RESOLUTION
                        ),
                        options = creatures,
                        minSelections = if (entersAsCopy.optional) 0 else 1,
                        maxSelections = 1,
                        useTargetingUI = true
                    )

                    // Push continuation
                    val continuation = CloneEntersContinuation(
                        decisionId = decisionId,
                        spellId = spellId,
                        controllerId = controllerId,
                        ownerId = ownerId,
                        castFaceDown = spellComponent.castFaceDown
                    )

                    val pausedState = state
                        .pushContinuation(continuation)
                        .withPendingDecision(decision)
                    return ExecutionResult.paused(pausedState, decision)
                }
                // No creatures on battlefield - fall through to enter as itself (0/0)
            }

            // Check for EntersWithColorChoice replacement effect (must be before creature type choice)
            val entersWithColorChoice = cardDef.script.replacementEffects.filterIsInstance<com.wingedsheep.sdk.scripting.EntersWithColorChoice>().firstOrNull()
            if (entersWithColorChoice != null) {
                val decisionId = "choose-color-enters-${spellId.value}"
                val decision = ChooseColorDecision(
                    id = decisionId,
                    playerId = controllerId,
                    prompt = "Choose a color",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent?.name,
                        phase = DecisionPhase.RESOLUTION
                    )
                )

                val continuation = ChooseColorEntersContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId
                )

                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                return ExecutionResult.paused(pausedState, decision)
            }

            // Check for EntersWithCreatureTypeChoice replacement effect
            val entersWithChoice = cardDef.script.replacementEffects.filterIsInstance<EntersWithCreatureTypeChoice>().firstOrNull()
            if (entersWithChoice != null) {
                val allCreatureTypes = com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES
                val chooserId = if (entersWithChoice.opponentChooses) {
                    state.turnOrder.firstOrNull { it != controllerId } ?: controllerId
                } else {
                    controllerId
                }
                val decisionId = "choose-creature-type-enters-${spellId.value}"
                val decision = ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose a creature type",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent?.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = allCreatureTypes,
                    defaultSearch = ""
                )

                val continuation = ChooseCreatureTypeEntersContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    creatureTypes = allCreatureTypes
                )

                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                return ExecutionResult.paused(pausedState, decision)
            }
        }

        // Normal permanent entry
        val newState = enterPermanentOnBattlefield(state, spellId, spellComponent, cardComponent, cardDef)
        return ExecutionResult.success(newState)
    }

    /**
     * Complete the permanent entry to the battlefield (shared between normal resolution and clone continuation).
     */
    internal fun enterPermanentOnBattlefield(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?
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
                updated = staticAbilityHandler.addReplacementEffectComponent(updated)
            }

            // Aura attachment: add AttachedToComponent pointing to the target
            if (auraTargetId != null) {
                updated = updated.with(
                    com.wingedsheep.engine.state.components.battlefield.AttachedToComponent(auraTargetId)
                )
            }

            updated
        }

        // Handle "enters with counters" replacement effects (before adding to battlefield)
        if (cardDef != null && !spellComponent.castFaceDown) {
            newState = applyEntersWithCounters(newState, spellId, cardDef, controllerId, spellComponent.xValue)
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
                sacrificedPermanentSubtypes = spellComponent.sacrificedPermanentSubtypes,
                damageDistribution = spellComponent.damageDistribution,
                chosenCreatureType = spellComponent.chosenCreatureType
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

        // Validate targets (including protection check - Rule 702.16)
        val sourceCard = state.getEntity(abilityComponent.sourceId)?.get<CardComponent>()
        val sourceColors = sourceCard?.colors ?: emptySet()
        val sourceSubtypes = sourceCard?.typeLine?.subtypes?.map { it.value }?.toSet() ?: emptySet()
        if (targetsComponent != null && targetsComponent.targets.isNotEmpty()) {
            val validTargets = validateTargets(
                state, targetsComponent.targets, sourceColors, sourceSubtypes,
                abilityComponent.controllerId, targetsComponent.targetRequirements,
                sourceId = abilityComponent.sourceId
            )
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

        // Validate targets (including protection check - Rule 702.16)
        val sourceCard = state.getEntity(abilityComponent.sourceId)?.get<CardComponent>()
        val sourceColors = sourceCard?.colors ?: emptySet()
        val sourceSubtypes = sourceCard?.typeLine?.subtypes?.map { it.value }?.toSet() ?: emptySet()
        if (targetsComponent != null && targetsComponent.targets.isNotEmpty()) {
            val validTargets = validateTargets(
                state, targetsComponent.targets, sourceColors, sourceSubtypes,
                abilityComponent.controllerId, targetsComponent.targetRequirements,
                sourceId = abilityComponent.sourceId
            )
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
            targets = targetsComponent?.targets ?: emptyList(),
            sacrificedPermanents = abilityComponent.sacrificedPermanents,
            xValue = abilityComponent.xValue,
            tappedPermanents = abilityComponent.tappedPermanents
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
    // Enters With Counters
    // =========================================================================

    private val dynamicAmountEvaluator = DynamicAmountEvaluator()

    /**
     * Apply "enters with counters" replacement effects to a permanent.
     * Handles both fixed count (EntersWithCounters) and dynamic count (EntersWithDynamicCounters).
     */
    internal fun applyEntersWithCounters(
        state: GameState,
        entityId: EntityId,
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        controllerId: EntityId,
        xValue: Int? = null
    ): GameState {
        var newState = state
        for (effect in cardDef.script.replacementEffects) {
            when (effect) {
                is EntersWithCounters -> {
                    val counterType = resolveCounterType(effect.counterType)
                    val current = newState.getEntity(entityId)?.get<CountersComponent>() ?: CountersComponent()
                    newState = newState.updateEntity(entityId) { c ->
                        c.with(current.withAdded(counterType, effect.count))
                    }
                }
                is EntersWithDynamicCounters -> {
                    val counterType = resolveCounterType(effect.counterType)
                    val context = EffectContext(
                        sourceId = entityId,
                        controllerId = controllerId,
                        opponentId = newState.turnOrder.firstOrNull { it != controllerId },
                        xValue = xValue
                    )
                    val count = dynamicAmountEvaluator.evaluate(newState, effect.count, context)
                    if (count > 0) {
                        val current = newState.getEntity(entityId)?.get<CountersComponent>() ?: CountersComponent()
                        newState = newState.updateEntity(entityId) { c ->
                            c.with(current.withAdded(counterType, count))
                        }
                    }
                }
                else -> { /* Other replacement effects handled elsewhere */ }
            }
        }
        return newState
    }

    private fun resolveCounterType(filter: CounterTypeFilter): CounterType {
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
     *
     * Checks zone existence, protection (Rule 702.16), and target filter matching
     * (Rule 608.2b — targets must still be legal when the spell/ability resolves).
     */
    private fun validateTargets(
        state: GameState,
        targets: List<ChosenTarget>,
        sourceColors: Set<Color> = emptySet(),
        sourceSubtypes: Set<String> = emptySet(),
        controllerId: EntityId,
        targetRequirements: List<TargetRequirement> = emptyList(),
        sourceId: EntityId? = null
    ): List<ChosenTarget> {
        // Always project state for shroud/hexproof checks (Rule 702.18, 702.11)
        val projected = stateProjector.project(state)
        val predicateContext = PredicateContext(controllerId = controllerId, sourceId = sourceId)

        return targets.filterIndexed { index, target ->
            when (target) {
                is ChosenTarget.Player -> {
                    // Player is valid if they exist and haven't lost
                    state.hasEntity(target.playerId)
                }

                is ChosenTarget.Permanent -> {
                    // Permanent is valid if still on battlefield
                    if (target.entityId !in state.getBattlefield()) return@filterIndexed false

                    // Check shroud — can't be targeted by anyone (Rule 702.18)
                    if (projected.hasKeyword(target.entityId, "SHROUD")) return@filterIndexed false

                    // Check hexproof — can't be targeted by opponents (Rule 702.11)
                    val entityController = state.getEntity(target.entityId)?.get<ControllerComponent>()?.playerId
                    if (projected.hasKeyword(target.entityId, "HEXPROOF") && entityController != controllerId) return@filterIndexed false

                    // Check protection from source colors/subtypes (Rule 702.16)
                    for (color in sourceColors) {
                        if (projected.hasKeyword(target.entityId, "PROTECTION_FROM_${color.name}")) {
                            return@filterIndexed false
                        }
                    }
                    for (subtype in sourceSubtypes) {
                        if (projected.hasKeyword(target.entityId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")) {
                            return@filterIndexed false
                        }
                    }

                    // Re-validate target filter (Rule 608.2b)
                    val requirement = getRequirementForTargetIndex(index, targetRequirements)
                    val filter = extractTargetFilter(requirement)
                    if (filter != null) {
                        if (!predicateEvaluator.matchesWithProjection(
                                state, projected, target.entityId, filter.baseFilter, predicateContext
                            )
                        ) {
                            return@filterIndexed false
                        }
                    }

                    true
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

    /**
     * Find the TargetRequirement that corresponds to a given target index.
     * Requirements are matched to targets in order, with each requirement
     * consuming `count` targets.
     */
    private fun getRequirementForTargetIndex(
        targetIndex: Int,
        requirements: List<TargetRequirement>
    ): TargetRequirement? {
        var idx = 0
        for (req in requirements) {
            val end = idx + req.count
            if (targetIndex in idx until end) return req
            idx = end
        }
        return null
    }

    /**
     * Extract the TargetFilter from a TargetRequirement, if it has one.
     */
    private fun extractTargetFilter(requirement: TargetRequirement?): TargetFilter? {
        return when (requirement) {
            is TargetCreature -> requirement.filter
            is TargetPermanent -> requirement.filter
            is TargetObject -> requirement.filter
            is TargetSpell -> requirement.filter
            else -> null
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

        // Check library (for Future Sight / play from top of library)
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        if (cardId in state.getZone(libraryZone)) {
            return state.removeFromZone(libraryZone, cardId)
        }

        return state
    }
}
