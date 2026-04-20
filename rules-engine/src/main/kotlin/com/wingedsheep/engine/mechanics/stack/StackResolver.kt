package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.EffectHandler
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TargetedByControllerThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.battlefield.CastFromHandComponent
import com.wingedsheep.engine.state.components.battlefield.WarpedComponent
import com.wingedsheep.engine.state.components.battlefield.WasKickedComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.ExileAfterResolveComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.WarpExileEffect
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.engine.handlers.effects.EntersWithCountersHelper
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.state.components.battlefield.CantBeTargetedByOpponentAbilitiesComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
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
    private val cardRegistry: CardRegistry,
    private val effectHandler: EffectHandler = EffectHandler(cardRegistry = cardRegistry),
    private val staticAbilityHandler: StaticAbilityHandler = StaticAbilityHandler(cardRegistry),
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
        sacrificedPermanents: List<PermanentSnapshot> = emptyList(),
        castFaceDown: Boolean = false,
        damageDistribution: Map<EntityId, Int>? = null,
        targetRequirements: List<TargetRequirement> = emptyList(),
        chosenCreatureType: String? = null,
        exiledCardCount: Int = 0,
        wasKicked: Boolean = false,
        wasWarped: Boolean = false,
        wasEvoked: Boolean = false,
        chosenModes: List<Int> = emptyList(),
        modeTargetsOrdered: List<List<ChosenTarget>> = emptyList(),
        modeTargetRequirements: Map<Int, List<TargetRequirement>> = emptyMap(),
        modeDamageDistribution: Map<Int, Map<EntityId, Int>> = emptyMap(),
        totalManaSpent: Int = 0,
        beheldCards: List<EntityId> = emptyList(),
        manaSpentWhite: Int = 0,
        manaSpentBlue: Int = 0,
        manaSpentBlack: Int = 0,
        manaSpentRed: Int = 0,
        manaSpentGreen: Int = 0,
        manaSpentColorless: Int = 0
    ): ExecutionResult {
        val container = state.getEntity(cardId)
            ?: return ExecutionResult.error(state, "Card not found: $cardId")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card: $cardId")

        // Determine which zone the spell is being cast from (before removal)
        val castFromZone = findCastFromZone(state, cardId, casterId)

        // Remove from current zone (typically hand)
        var newState = removeFromCurrentZone(state, cardId, casterId)

        // Build the flat target union for choose-N modal spells (Rule 700.2 / 601.2c).
        // TargetsComponent holds the union so existing target-arrow rendering and resolution-time
        // re-validation keep working; per-mode breakdown lives on SpellOnStackComponent.
        val effectiveTargets = if (modeTargetsOrdered.isNotEmpty()) {
            modeTargetsOrdered.flatten()
        } else {
            targets
        }
        val effectiveTargetRequirements = if (modeTargetRequirements.isNotEmpty() && targetRequirements.isEmpty()) {
            chosenModes.flatMap { modeTargetRequirements[it] ?: emptyList() }
        } else {
            targetRequirements
        }

        // Add spell components
        newState = newState.updateEntity(cardId) { c ->
            var updated = c.with(SpellOnStackComponent(
                casterId = casterId,
                xValue = xValue,
                wasKicked = wasKicked,
                chosenModes = chosenModes,
                modeTargetsOrdered = modeTargetsOrdered,
                modeTargetRequirements = modeTargetRequirements,
                modeDamageDistribution = modeDamageDistribution,
                sacrificedPermanents = sacrificedPermanents,
                castFaceDown = castFaceDown,
                damageDistribution = damageDistribution,
                chosenCreatureType = chosenCreatureType,
                exiledCardCount = exiledCardCount,
                castFromZone = castFromZone,
                wasWarped = wasWarped,
                wasEvoked = wasEvoked,
                beheldCards = beheldCards,
                manaSpentWhite = manaSpentWhite,
                manaSpentBlue = manaSpentBlue,
                manaSpentBlack = manaSpentBlack,
                manaSpentRed = manaSpentRed,
                manaSpentGreen = manaSpentGreen,
                manaSpentColorless = manaSpentColorless
            ))
            if (effectiveTargets.isNotEmpty()) {
                updated = updated.with(TargetsComponent(effectiveTargets, effectiveTargetRequirements))
            }
            // Add morph data for creatures with morph (needed for face-down casting and
            // for effects like Backslide that target "creature with a morph ability")
            val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
            val morphAbility = cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Morph>()?.firstOrNull()
            if (morphAbility != null) {
                updated = updated.with(MorphDataComponent(
                    morphCost = morphAbility.morphCost,
                    originalCardDefinitionId = cardComponent.cardDefinitionId,
                    faceUpEffect = morphAbility.faceUpEffect
                ))
            }
            updated
        }

        // Push to stack and reset priority passes (new stack item requires fresh round of passes)
        newState = newState.pushToStack(cardId)
            .copy(priorityPassedBy = emptySet())

        // Consume one-shot free-cast permissions used to play this spell. If the
        // spell is later countered or fizzles and ExileAfterResolveComponent sends
        // it back to exile, the permission must already be gone — otherwise the
        // controller could re-cast the same card repeatedly (e.g. Daring Waverider's
        // free cast resurfacing every time the granted spell is countered).
        // "Permanent" permissions (e.g. Kheru Spellsnatcher's "for as long as it
        // remains exiled" grant) are left intact.
        newState = newState.updateEntity(cardId) { c ->
            var updated = c
            val mayPlay = c.get<MayPlayFromExileComponent>()
            if (mayPlay != null && !mayPlay.permanent) {
                updated = updated.without<MayPlayFromExileComponent>()
            }
            val payCost = c.get<PlayWithoutPayingCostComponent>()
            if (payCost != null && !payCost.permanent) {
                updated = updated.without<PlayWithoutPayingCostComponent>()
            }
            updated
        }

        // For face-down creatures, use a generic name in the event
        val eventName = if (castFaceDown) "Face-down creature" else cardComponent.name

        // Collect target names for the cast event log
        val targetNames = effectiveTargets.mapNotNull { target ->
            when (target) {
                is ChosenTarget.Permanent -> newState.getEntity(target.entityId)?.get<CardComponent>()?.name
                is ChosenTarget.Player -> if (target.playerId == casterId) "themselves" else "opponent"
                is ChosenTarget.Spell -> newState.getEntity(target.spellEntityId)?.get<CardComponent>()?.name
                    ?: "spell"
                is ChosenTarget.Card -> newState.getEntity(target.cardId)?.get<CardComponent>()?.name
            }
        }

        val events = mutableListOf<GameEvent>(SpellCastEvent(cardId, eventName, casterId, targetNames, xValue, wasKicked, totalManaSpent))

        // Emit BecomesTargetEvent for each permanent target (Rule 601.2c)
        // Also track targeting for Valiant ("first time each turn")
        for (target in effectiveTargets) {
            if (target is ChosenTarget.Permanent) {
                val targetName = newState.getEntity(target.entityId)?.get<CardComponent>()?.name ?: "Unknown"
                val firstTime = !hasBeenTargetedByController(newState, target.entityId, casterId)
                events.add(BecomesTargetEvent(target.entityId, targetName, cardId, casterId, firstTime))
                newState = markTargetedByController(newState, target.entityId, casterId)
            }
        }

        return ExecutionResult.success(
            newState.tick(),
            events
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

        val events = mutableListOf<GameEvent>(
            AbilityTriggeredEvent(
                ability.sourceId,
                ability.sourceName,
                ability.controllerId,
                ability.description,
                abilityEntityId = abilityId
            )
        )

        // Emit BecomesTargetEvent for each permanent target
        // Use abilityId (the entity on the stack) as source so ward can counter it
        for (target in targets) {
            if (target is ChosenTarget.Permanent) {
                val targetName = newState.getEntity(target.entityId)?.get<CardComponent>()?.name ?: "Unknown"
                val firstTime = !hasBeenTargetedByController(newState, target.entityId, ability.controllerId)
                events.add(BecomesTargetEvent(target.entityId, targetName, abilityId, ability.controllerId, firstTime))
                newState = markTargetedByController(newState, target.entityId, ability.controllerId)
            }
        }

        return ExecutionResult.success(
            newState.tick(),
            events
        )
    }

    /**
     * Put a copy of a spell on the stack.
     *
     * Per rule 707.7/707.12, a copy of an instant or sorcery spell is itself a spell on the
     * stack with the original's characteristics. We clone the source's [CardComponent] and
     * [SpellOnStackComponent] onto a new entity, tag it with [CopyOfComponent], and push it.
     *
     * Per rule 707.10 a copy isn't cast — this emits a [SpellCopiedEvent], not a
     * [SpellCastEvent], so "whenever you cast a spell" triggers don't fire.
     *
     * Targets and modal choices default to inheriting from the source. Callers may override
     * them (e.g., Storm's per-copy retargeting).
     */
    fun putSpellCopy(
        state: GameState,
        sourceSpellId: EntityId,
        targets: List<ChosenTarget> = emptyList(),
        targetRequirements: List<TargetRequirement> = emptyList(),
        chosenModes: List<Int>? = null,
        modeTargetsOrdered: List<List<ChosenTarget>>? = null,
        modeTargetRequirements: Map<Int, List<TargetRequirement>>? = null,
        copyIndex: Int? = null,
        copyTotal: Int? = null,
        controllerId: EntityId? = null
    ): ExecutionResult {
        val sourceContainer = state.getEntity(sourceSpellId)
            ?: return ExecutionResult.error(state, "Source spell not found: $sourceSpellId")
        val sourceCard = sourceContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Source is not a card: $sourceSpellId")
        val sourceSpell = sourceContainer.get<SpellOnStackComponent>()
            ?: return ExecutionResult.error(state, "Source is not a spell on stack: $sourceSpellId")
        val sourceTargets = sourceContainer.get<TargetsComponent>()

        val copyId = EntityId.generate()
        val copyController = controllerId ?: sourceSpell.casterId

        val effectiveModes = chosenModes ?: sourceSpell.chosenModes
        val effectiveModeTargets = modeTargetsOrdered ?: sourceSpell.modeTargetsOrdered
        val effectiveModeRequirements = modeTargetRequirements ?: sourceSpell.modeTargetRequirements

        // Determine final flat targets/requirements for the copy's TargetsComponent.
        val effectiveTargets = when {
            targets.isNotEmpty() -> targets
            effectiveModes.isNotEmpty() -> effectiveModeTargets.flatten()
            else -> sourceTargets?.targets ?: emptyList()
        }
        val effectiveRequirements = when {
            targetRequirements.isNotEmpty() -> targetRequirements
            effectiveModes.isNotEmpty() ->
                effectiveModes.flatMap { effectiveModeRequirements[it] ?: emptyList() }
            else -> sourceTargets?.targetRequirements ?: emptyList()
        }

        // Clone the card characteristics. The CardComponent keeps the same cardDefinitionId,
        // name, types, colors, mana cost, and spellEffect (707.7c / 707.12).
        val copiedCardComp = sourceCard.copy(ownerId = copyController)

        // Clone cast-time state; per 707.7c the copy inherits every decision made for
        // the original. The data-class copy preserves: xValue, wasKicked, wasWarped,
        // wasEvoked, sacrificedPermanents (snapshots of P/T + subtypes), damageDistribution,
        // chosenCreatureType, exiledCardCount, castFromZone, beheldCards, and the
        // manaSpent{White,Blue,Black,Red,Green,Colorless} colors. Only the caster
        // (copy controller) and modal fields (which the caller may retarget) are
        // overridden explicitly. Payment events (ManaSpentEvent, SpellCastEvent) are
        // deliberately not re-emitted — a copy isn't cast (707.10).
        val copiedSpellComp = sourceSpell.copy(
            casterId = copyController,
            chosenModes = effectiveModes,
            modeTargetsOrdered = effectiveModeTargets,
            modeTargetRequirements = effectiveModeRequirements
        )

        var container = ComponentContainer.of(copiedCardComp, copiedSpellComp)
        if (effectiveTargets.isNotEmpty()) {
            container = container.with(TargetsComponent(effectiveTargets, effectiveRequirements))
        }
        container = container.with(
            CopyOfComponent(
                originalCardDefinitionId = sourceCard.cardDefinitionId,
                copiedCardDefinitionId = sourceCard.cardDefinitionId
            )
        )

        var newState = state.withEntity(copyId, container)
        newState = newState.pushToStack(copyId).copy(priorityPassedBy = emptySet())

        val events = mutableListOf<GameEvent>(
            SpellCopiedEvent(
                copyEntityId = copyId,
                cardName = sourceCard.name,
                controllerId = copyController,
                originalSpellId = sourceSpellId,
                copyIndex = copyIndex,
                copyTotal = copyTotal
            )
        )

        // Emit BecomesTargetEvent for each permanent target — the copy is its own source
        // on the stack (ward on the target can counter the copy independently).
        for (target in effectiveTargets) {
            if (target is ChosenTarget.Permanent) {
                val targetName = newState.getEntity(target.entityId)?.get<CardComponent>()?.name ?: "Unknown"
                val firstTime = !hasBeenTargetedByController(newState, target.entityId, copyController)
                events.add(BecomesTargetEvent(target.entityId, targetName, copyId, copyController, firstTime))
                newState = markTargetedByController(newState, target.entityId, copyController)
            }
        }

        return ExecutionResult.success(newState.tick(), events)
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

        val events = mutableListOf<GameEvent>(
            AbilityActivatedEvent(
                ability.sourceId,
                ability.sourceName,
                ability.controllerId,
                abilityEntityId = abilityId
            )
        )

        // Emit BecomesTargetEvent for each permanent target
        // Use abilityId (the entity on the stack) as source so ward can counter it
        for (target in targets) {
            if (target is ChosenTarget.Permanent) {
                val targetName = newState.getEntity(target.entityId)?.get<CardComponent>()?.name ?: "Unknown"
                val firstTime = !hasBeenTargetedByController(newState, target.entityId, ability.controllerId)
                events.add(BecomesTargetEvent(target.entityId, targetName, abilityId, ability.controllerId, firstTime))
                newState = markTargetedByController(newState, target.entityId, ability.controllerId)
            }
        }

        return ExecutionResult.success(
            newState.tick(),
            events
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
                sourceId = spellId,
                targetingSourceType = TargetingSourceType.SPELL
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
            events.add(ResolvedEvent(spellId, cardComponent.name))
            events.add(
                ZoneChangeEvent(
                    spellId,
                    cardComponent.name,
                    null, // Was on stack
                    Zone.BATTLEFIELD,
                    cardComponent.ownerId ?: spellComponent.casterId,
                    xValue = spellComponent.xValue
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
        val cardDef = cardComponent?.cardDefinitionId?.let { cardRegistry.getCard(it) }
        if (cardDef != null && !spellComponent.castFaceDown) {
            val entersAsCopy = cardDef.script.replacementEffects.filterIsInstance<EntersAsCopy>().firstOrNull()
            if (entersAsCopy != null) {
                // Find all permanents on the battlefield matching the copy filter
                val copyFilter = entersAsCopy.copyFilter
                var candidates = state.getBattlefield().filter { entityId ->
                    predicateEvaluator.matches(
                        state, entityId, copyFilter,
                        PredicateContext(controllerId = controllerId)
                    )
                }

                // Filter by mana value ≤ total mana spent (for Mockingbird-style effects)
                if (entersAsCopy.filterByTotalManaSpent) {
                    val xValue = spellComponent.xValue ?: 0
                    // Total mana spent = X + non-X portion of mana cost
                    val baseNonXCost = cardComponent.manaCost.symbols
                        .filterNot { it is com.wingedsheep.sdk.core.ManaSymbol.X }
                        .sumOf { it.cmc }
                    val totalManaSpent = xValue + baseNonXCost
                    candidates = candidates.filter { entityId ->
                        val targetCard = state.getEntity(entityId)?.get<CardComponent>()
                        (targetCard?.manaValue ?: 0) <= totalManaSpent
                    }
                }

                if (candidates.isNotEmpty()) {
                    // Present the selection decision
                    val filterDesc = copyFilter.description
                    val decisionId = "clone-enters-${spellId.value}"
                    val decision = SelectCardsDecision(
                        id = decisionId,
                        playerId = controllerId,
                        prompt = if (entersAsCopy.optional) {
                            "You may choose a $filterDesc to copy"
                        } else {
                            "Choose a $filterDesc to copy"
                        },
                        context = DecisionContext(
                            sourceId = spellId,
                            sourceName = cardComponent.name,
                            phase = DecisionPhase.RESOLUTION
                        ),
                        options = candidates,
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
                        castFaceDown = spellComponent.castFaceDown,
                        additionalSubtypes = entersAsCopy.additionalSubtypes,
                        additionalKeywords = entersAsCopy.additionalKeywords
                    )

                    val pausedState = state
                        .pushContinuation(continuation)
                        .withPendingDecision(decision)
                    return ExecutionResult.paused(pausedState, decision)
                }
                // No matching permanents on battlefield - fall through to enter as itself (0/0)
            }

            // Check for EntersWithChoice replacement effects (color first, then creature type, then creature)
            // Process in priority order: COLOR → CREATURE_TYPE → CREATURE_ON_BATTLEFIELD
            // When a card has multiple choices (e.g., Riptide Replicator: color + creature type),
            // the first one pauses; its continuation resumer chains to the next.
            val entersWithChoices = cardDef.script.replacementEffects.filterIsInstance<EntersWithChoice>()
            val firstChoice = entersWithChoices
                .sortedBy { it.choiceType.ordinal }
                .firstOrNull()
            if (firstChoice != null) {
                val result = pauseForEntersWithChoice(state, spellId, controllerId, ownerId, cardComponent, firstChoice)
                if (result != null) return result
                // null means choice couldn't be presented (e.g., no creatures on battlefield) — fall through
            }

            // Check for EntersWithRevealCounters replacement effect (Amplify mechanic)
            val revealCountersEffect = cardDef.script.replacementEffects.filterIsInstance<com.wingedsheep.sdk.scripting.EntersWithRevealCounters>().firstOrNull()
            if (revealCountersEffect != null) {
                // Find cards in the reveal source zone that match the effect's filter
                val revealZone = ZoneKey(controllerId, revealCountersEffect.revealSource)
                val predicateContext = PredicateContext(controllerId = controllerId, sourceId = spellId)
                val validCards = state.getZone(revealZone).filter { cardId ->
                    predicateEvaluator.matches(state, cardId, revealCountersEffect.filter, predicateContext)
                }

                if (validCards.isNotEmpty()) {
                    val decisionId = "reveal-counters-enters-${spellId.value}"
                    val decision = SelectCardsDecision(
                        id = decisionId,
                        playerId = controllerId,
                        prompt = "Reveal cards from your ${revealCountersEffect.revealSource.name.lowercase()} that match ${cardComponent.name} (${revealCountersEffect.countersPerReveal} ${revealCountersEffect.counterType} counter${if (revealCountersEffect.countersPerReveal > 1) "s" else ""} each)",
                        context = DecisionContext(
                            sourceId = spellId,
                            sourceName = cardComponent.name,
                            phase = DecisionPhase.RESOLUTION
                        ),
                        options = validCards,
                        minSelections = 0,
                        maxSelections = validCards.size
                    )

                    val continuation = RevealCountersContinuation(
                        decisionId = decisionId,
                        spellId = spellId,
                        controllerId = controllerId,
                        ownerId = ownerId,
                        counterType = revealCountersEffect.counterType,
                        countersPerReveal = revealCountersEffect.countersPerReveal
                    )

                    val pausedState = state
                        .pushContinuation(continuation)
                        .withPendingDecision(decision)
                    return ExecutionResult.paused(pausedState, decision)
                }
                // No valid cards — enter normally without counters
            }
        }

        // Check for "pay life or enter tapped" (shock lands) before entering the battlefield
        if (cardDef != null && !spellComponent.castFaceDown) {
            val entersTapped = cardDef.script.replacementEffects.filterIsInstance<EntersTapped>().firstOrNull()
            if (entersTapped?.payLifeCost != null) {
                val decisionId = "pay-life-or-enter-tapped-spell-${spellId.value}"
                val decision = YesNoDecision(
                    id = decisionId,
                    playerId = controllerId,
                    prompt = "Pay ${entersTapped.payLifeCost} life to have ${cardComponent.name} enter untapped?",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    )
                )
                val continuation = PayLifeOrEnterTappedSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    lifeCost = entersTapped.payLifeCost!!
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                return ExecutionResult.paused(pausedState, decision)
            }
        }

        // Normal permanent entry
        val (newState, enterEvents) = enterPermanentOnBattlefield(state, spellId, spellComponent, cardComponent, cardDef)
        val sagaEvents = if (cardDef != null && !spellComponent.castFaceDown && cardDef.isSaga) {
            listOf(CountersAddedEvent(spellId, "LORE", 1, cardDef.name))
        } else {
            emptyList()
        }
        return ExecutionResult.success(newState, enterEvents + sagaEvents)
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
    ): Pair<GameState, List<GameEvent>> {
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

            // If cast face-down (morph), add FaceDownComponent and strip any
            // RevealedToComponent from hand-peek effects (zone change = new object)
            // MorphDataComponent was already added when the spell was cast
            if (spellComponent.castFaceDown) {
                updated = updated.with(FaceDownComponent)
                    .without<RevealedToComponent>()
            }

            // Creatures enter with summoning sickness (including face-down creatures)
            if (cardComponent?.typeLine?.isCreature == true || spellComponent.castFaceDown) {
                updated = updated.with(SummoningSicknessComponent)
            }

            // Track that this permanent entered the battlefield this turn
            updated = updated.with(EnteredThisTurnComponent)

            // Track if this permanent was cast from hand (for cards like Phage the Untouchable)
            if (spellComponent.castFromZone == Zone.HAND) {
                updated = updated.with(CastFromHandComponent)
            }

            // Track if this permanent was kicked (for cards like Skizzik)
            if (spellComponent.wasKicked) {
                updated = updated.with(WasKickedComponent)
            }

            // Track if this permanent was cast for its warp cost
            if (spellComponent.wasWarped) {
                updated = updated.with(WarpedComponent)
            }

            // Track if this permanent was cast for its evoke cost
            if (spellComponent.wasEvoked) {
                updated = updated.with(com.wingedsheep.engine.state.components.battlefield.EvokedComponent)
            }

            // Record mana colors spent to cast (for mana-spent-gated triggers)
            if (spellComponent.manaSpentWhite > 0 || spellComponent.manaSpentBlue > 0 ||
                spellComponent.manaSpentBlack > 0 || spellComponent.manaSpentRed > 0 ||
                spellComponent.manaSpentGreen > 0 || spellComponent.manaSpentColorless > 0) {
                updated = updated.with(com.wingedsheep.engine.state.components.battlefield.CastRecordComponent(
                    whiteSpent = spellComponent.manaSpentWhite,
                    blueSpent = spellComponent.manaSpentBlue,
                    blackSpent = spellComponent.manaSpentBlack,
                    redSpent = spellComponent.manaSpentRed,
                    greenSpent = spellComponent.manaSpentGreen,
                    colorlessSpent = spellComponent.manaSpentColorless
                ))
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

        // Aura: add reverse AttachmentsComponent on the enchanted permanent
        if (auraTargetId != null) {
            newState = newState.updateEntity(auraTargetId) { container ->
                val existing = container.get<com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent>()
                val updatedIds = (existing?.attachedIds ?: emptyList()) + spellId
                container.with(com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent(updatedIds))
            }
        }

        // Handle "enters the battlefield tapped" replacement effect
        // Note: payLifeCost shock lands are handled in resolvePermanentSpell before this method is called.
        if (cardDef != null && !spellComponent.castFaceDown) {
            val entersTapped = cardDef.script.replacementEffects.filterIsInstance<EntersTapped>().firstOrNull()
            if (entersTapped != null && entersTapped.payLifeCost == null) {
                val shouldEnterTapped = if (entersTapped.unlessCondition != null) {
                    val context = EffectContext(
                        sourceId = spellId,
                        controllerId = controllerId,
                        opponentId = newState.turnOrder.firstOrNull { it != controllerId }
                    )
                    !com.wingedsheep.engine.handlers.ConditionEvaluator().evaluate(
                        newState, entersTapped.unlessCondition!!, context
                    )
                } else {
                    true
                }
                if (shouldEnterTapped) {
                    newState = newState.updateEntity(spellId) { c -> c.with(TappedComponent) }
                }
            }
        }

        // Handle "enters with counters" replacement effects (before adding to battlefield)
        val counterEvents = mutableListOf<GameEvent>()
        if (cardDef != null && !spellComponent.castFaceDown) {
            val (counterState, events) = applyEntersWithCounters(newState, spellId, cardDef, controllerId, spellComponent.xValue)
            newState = counterState
            counterEvents.addAll(events)
        }

        // Handle planeswalker starting loyalty (Rule 306.5b)
        if (cardDef != null && !spellComponent.castFaceDown && cardDef.startingLoyalty != null) {
            val loyaltyCount = cardDef.startingLoyalty!!
            val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                newState, spellId, CounterType.LOYALTY, loyaltyCount
            )
            val current = newState.getEntity(spellId)?.get<CountersComponent>() ?: CountersComponent()
            newState = newState.updateEntity(spellId) { c ->
                c.with(current.withAdded(CounterType.LOYALTY, modifiedCount))
            }
        }

        // Handle Class entering the battlefield (Rule 716)
        // Add ClassLevelComponent starting at level 1
        if (cardDef != null && !spellComponent.castFaceDown && cardDef.isClass) {
            newState = newState.updateEntity(spellId) { c ->
                c.with(ClassLevelComponent(currentLevel = 1))
            }
        }

        // Handle double-faced cards entering the battlefield (Rule 712)
        // DFCs always enter on their front face (Rule 712.4).
        if (cardDef != null && !spellComponent.castFaceDown && cardDef.isDoubleFaced) {
            val backFace = cardDef.backFace!!
            newState = newState.updateEntity(spellId) { c ->
                c.with(
                    com.wingedsheep.engine.state.components.identity.DoubleFacedComponent(
                        frontCardDefinitionId = cardDef.name,
                        backCardDefinitionId = backFace.name,
                        currentFace = com.wingedsheep.engine.state.components.identity.DoubleFacedComponent.Face.FRONT
                    )
                )
            }
        }

        // Handle Saga entering the battlefield (Rule 714.3a)
        // Add SagaComponent and initial lore counter (triggers chapter I detection)
        if (cardDef != null && !spellComponent.castFaceDown && cardDef.isSaga) {
            val current = newState.getEntity(spellId)?.get<CountersComponent>() ?: CountersComponent()
            // Mark chapter 1 as triggered since lore count will be 1
            val sagaComponent = SagaComponent(triggeredChapters = setOf(1))
            newState = newState.updateEntity(spellId) { c ->
                c.with(sagaComponent)
                    .with(current.withAdded(CounterType.LORE, 1))
            }
        }

        // Add to battlefield
        val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, spellId)

        // Warp: create delayed trigger to exile at beginning of next end step
        if (spellComponent.wasWarped) {
            val delayedTrigger = DelayedTriggeredAbility(
                id = java.util.UUID.randomUUID().toString(),
                effect = WarpExileEffect(EffectTarget.SpecificEntity(spellId)),
                fireAtStep = Step.END,
                sourceId = spellId,
                sourceName = cardComponent?.name ?: "Unknown",
                controllerId = controllerId
            )
            newState = newState.addDelayedTrigger(delayedTrigger)
        }

        return newState to counterEvents
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
        // Use kickerSpellEffect when the spell was kicked and an alternate effect is defined
        val baseSpellEffect = if (spellComponent.wasKicked && cardComponent != null) {
            val cardDef = cardRegistry.getCard(cardComponent.name)
            cardDef?.script?.kickerSpellEffect ?: cardComponent.spellEffect
        } else {
            cardComponent?.spellEffect
        }
        val rawSpellEffect = baseSpellEffect
        val textReplacement = state.getEntity(spellId)?.get<TextReplacementComponent>()
        val spellEffect = if (rawSpellEffect != null && textReplacement != null) {
            rawSpellEffect.applyTextReplacement(textReplacement)
        } else {
            rawSpellEffect
        }
        if (spellEffect != null) {
            val targetRequirements = state.getEntity(spellId)?.get<TargetsComponent>()?.targetRequirements ?: emptyList()
            val context = EffectContext(
                sourceId = spellId,
                controllerId = spellComponent.casterId,
                opponentId = newState.getOpponent(spellComponent.casterId),
                targets = targets,
                xValue = spellComponent.xValue,
                wasKicked = spellComponent.wasKicked,
                sacrificedPermanents = spellComponent.sacrificedPermanents,
                damageDistribution = spellComponent.damageDistribution,
                chosenModes = spellComponent.chosenModes,
                modeTargetsOrdered = spellComponent.modeTargetsOrdered,
                modeTargetRequirements = spellComponent.modeTargetRequirements,
                chosenCreatureType = spellComponent.chosenCreatureType,
                exiledCardCount = spellComponent.exiledCardCount,
                castFromZone = spellComponent.castFromZone,
                pipeline = PipelineState(
                    namedTargets = EffectContext.buildNamedTargets(targetRequirements, targets),
                    storedCollections = if (spellComponent.beheldCards.isNotEmpty())
                        mapOf("beheld" to spellComponent.beheldCards) else emptyMap()
                )
            )

            val effectResult = effectHandler.execute(newState, spellEffect, context)

            // If effect is paused awaiting a decision, we still need to move the spell
            // to graveyard/exile (it has already resolved from the stack). The decision only
            // determines how the effect completes.
            if (effectResult.isPaused) {
                val pausedIsCopy = effectResult.state.getEntity(spellId)?.has<CopyOfComponent>() == true
                if (pausedIsCopy) {
                    // Rule 112.3b — copies cease to exist when they leave the stack.
                    val pausedState = effectResult.state.removeEntity(spellId)
                    return ExecutionResult.paused(
                        pausedState,
                        effectResult.pendingDecision!!,
                        events + effectResult.events
                    )
                }

                val ownerId = cardComponent?.ownerId ?: spellComponent.casterId
                val pausedCardDef = cardComponent?.let { cardRegistry.getCard(it.name) }
                val pausedSelfExile = pausedCardDef?.script?.selfExileOnResolve == true
                val pausedFlashbackExile = spellComponent.castFromZone == Zone.GRAVEYARD &&
                    pausedCardDef?.keywordAbilities?.any { it is KeywordAbility.Flashback } == true
                val pausedExileAfterResolve = effectResult.state.getEntity(spellId)?.has<ExileAfterResolveComponent>() == true
                val pausedIntended = if (pausedSelfExile || pausedFlashbackExile || pausedExileAfterResolve) Zone.EXILE else Zone.GRAVEYARD

                // Apply RedirectZoneChange replacement effects (e.g., Festival of Embers).
                val pausedRedirect = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.checkZoneChangeRedirect(
                    effectResult.state, spellId, Zone.STACK, pausedIntended
                )
                val pausedDestZone = pausedRedirect.destinationZone
                val pausedDestZoneKey = ZoneKey(ownerId, pausedDestZone)

                // Move spell to graveyard/exile even though effect is paused
                var pausedState = effectResult.state.updateEntity(spellId) { c ->
                    c.without<SpellOnStackComponent>().without<TargetsComponent>()
                }
                pausedState = pausedState.addToZone(pausedDestZoneKey, spellId)

                pausedRedirect.additionalEffect?.let { extra ->
                    pausedState = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.applyReplacementAdditionalEffect(
                        pausedState, extra, pausedRedirect.effectControllerId, spellId
                    )
                }

                // Include the zone change event along with effect events
                val allEvents = events + effectResult.events + ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    null,
                    pausedDestZone,
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

        // Rule 112.3b: a copy of a spell ceases to exist when it leaves the stack —
        // it does not go to a graveyard or exile.
        val isCopy = newState.getEntity(spellId)?.has<CopyOfComponent>() == true
        if (isCopy) {
            newState = newState.removeEntity(spellId)
            return ExecutionResult.success(newState, events)
        }

        // Move to graveyard (or exile if selfExileOnResolve, flashback, or ExileAfterResolveComponent)
        val ownerId = cardComponent?.ownerId ?: spellComponent.casterId
        val cardDef = cardComponent?.let { cardRegistry.getCard(it.name) }
        val selfExile = cardDef?.script?.selfExileOnResolve == true
        val flashbackExile = spellComponent.castFromZone == Zone.GRAVEYARD &&
            cardDef?.keywordAbilities?.any { it is KeywordAbility.Flashback } == true
        val exileAfterResolve = newState.getEntity(spellId)?.has<ExileAfterResolveComponent>() == true
        val intendedDestination = if (selfExile || flashbackExile || exileAfterResolve) Zone.EXILE else Zone.GRAVEYARD

        // Apply RedirectZoneChange replacement effects (e.g., Festival of Embers
        // exiles cards that would go to your graveyard from anywhere).
        val redirect = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.checkZoneChangeRedirect(
            newState, spellId, Zone.STACK, intendedDestination
        )
        val destinationZone = redirect.destinationZone
        val destZoneKey = ZoneKey(ownerId, destinationZone)

        newState = newState.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>()
                .without<TargetsComponent>()
                .without<com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent>()
                .without<com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent>()
                .without<ExileAfterResolveComponent>()
        }
        newState = newState.addToZone(destZoneKey, spellId)

        redirect.additionalEffect?.let { extra ->
            newState = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.applyReplacementAdditionalEffect(
                newState, extra, redirect.effectControllerId, spellId
            )
        }

        events.add(
            ZoneChangeEvent(
                spellId,
                cardComponent?.name ?: "Unknown",
                null,
                destinationZone,
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
        // Rule 112.3b — a copy that fizzles ceases to exist rather than moving to graveyard/exile.
        val isCopy = state.getEntity(spellId)?.has<CopyOfComponent>() == true
        if (isCopy) {
            val newState = state.removeEntity(spellId)
            return ExecutionResult.success(
                newState,
                listOf(
                    SpellFizzledEvent(spellId, cardComponent?.name ?: "Unknown", "All targets are invalid")
                )
            )
        }

        val ownerId = cardComponent?.ownerId ?: spellComponent.casterId
        val cardDef = cardComponent?.let { cardRegistry.getCard(it.name) }
        val flashbackExile = spellComponent.castFromZone == Zone.GRAVEYARD &&
            cardDef?.keywordAbilities?.any { it is KeywordAbility.Flashback } == true
        val exileAfterResolve = state.getEntity(spellId)?.has<ExileAfterResolveComponent>() == true
        val destZone = if (flashbackExile || exileAfterResolve) Zone.EXILE else Zone.GRAVEYARD
        val destZoneKey = ZoneKey(ownerId, destZone)

        var newState = state.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>().without<TargetsComponent>()
        }
        newState = newState.addToZone(destZoneKey, spellId)

        return ExecutionResult.success(
            newState,
            listOf(
                SpellFizzledEvent(spellId, cardComponent?.name ?: "Unknown", "All targets are invalid"),
                ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    null,
                    destZone,
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
                sourceId = abilityComponent.sourceId,
                targetingSourceType = TargetingSourceType.ABILITY
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
        val resolvedTargets2 = targetsComponent?.targets ?: emptyList()
        val targetReqs = targetsComponent?.targetRequirements ?: emptyList()
        val context = EffectContext(
            sourceId = abilityComponent.sourceId,
            controllerId = abilityComponent.controllerId,
            opponentId = state.getOpponent(abilityComponent.controllerId),
            targets = resolvedTargets2,
            triggerDamageAmount = abilityComponent.triggerDamageAmount,
            triggerCounterCount = abilityComponent.triggerCounterCount,
            triggerTotalCounterCount = abilityComponent.triggerTotalCounterCount,
            triggeringEntityId = abilityComponent.triggeringEntityId,
            triggeringPlayerId = abilityComponent.triggeringPlayerId,
            targetingSourceEntityId = abilityComponent.targetingSourceEntityId,
            triggerLastKnownPower = abilityComponent.lastKnownPower,
            triggerLastKnownToughness = abilityComponent.lastKnownToughness,
            xValue = abilityComponent.xValue,
            damageDistribution = abilityComponent.damageDistribution,
            chosenModes = abilityComponent.chosenModes,
            modeTargetsOrdered = abilityComponent.modeTargetsOrdered,
            modeTargetRequirements = abilityComponent.modeTargetRequirements,
            pipeline = PipelineState(namedTargets = EffectContext.buildNamedTargets(targetReqs, resolvedTargets2))
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
        val activatedTargets = targetsComponent?.targets ?: emptyList()
        val activatedReqs = targetsComponent?.targetRequirements ?: emptyList()
        val context = EffectContext(
            sourceId = abilityComponent.sourceId,
            controllerId = abilityComponent.controllerId,
            opponentId = state.getOpponent(abilityComponent.controllerId),
            targets = activatedTargets,
            sacrificedPermanents = abilityComponent.sacrificedPermanents,
            xValue = abilityComponent.xValue,
            tappedPermanents = abilityComponent.tappedPermanents,
            pipeline = PipelineState(namedTargets = EffectContext.buildNamedTargets(activatedReqs, activatedTargets))
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
     * Also checks other battlefield permanents for replacement effects that modify entering creatures
     * (e.g., Gev, Scaled Scorch: "Other creatures you control enter with additional +1/+1 counters").
     */
    internal fun applyEntersWithCounters(
        state: GameState,
        entityId: EntityId,
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        controllerId: EntityId,
        xValue: Int? = null
    ): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()
        val entityName = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: ""
        // Apply the entering creature's own replacement effects
        for (effect in cardDef.script.replacementEffects) {
            when (effect) {
                is EntersWithCounters -> {
                    val counterType = resolveCounterType(effect.counterType)
                    val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                        newState, entityId, counterType, effect.count
                    )
                    val current = newState.getEntity(entityId)?.get<CountersComponent>() ?: CountersComponent()
                    newState = newState.updateEntity(entityId) { c ->
                        c.with(current.withAdded(counterType, modifiedCount))
                    }
                    events.add(CountersAddedEvent(entityId, effect.counterType.description, modifiedCount, entityName))
                }
                is EntersWithDynamicCounters -> {
                    // Skip "other only" effects when applying to self (e.g., Gev)
                    if (effect.otherOnly) continue
                    val counterType = resolveCounterType(effect.counterType)
                    val context = EffectContext(
                        sourceId = entityId,
                        controllerId = controllerId,
                        opponentId = newState.turnOrder.firstOrNull { it != controllerId },
                        xValue = xValue
                    )
                    val count = dynamicAmountEvaluator.evaluate(newState, effect.count, context)
                    if (count > 0) {
                        val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                            newState, entityId, counterType, count
                        )
                        val current = newState.getEntity(entityId)?.get<CountersComponent>() ?: CountersComponent()
                        newState = newState.updateEntity(entityId) { c ->
                            c.with(current.withAdded(counterType, modifiedCount))
                        }
                        events.add(CountersAddedEvent(entityId, effect.counterType.description, modifiedCount, entityName))
                    }
                }
                else -> { /* Other replacement effects handled elsewhere */ }
            }
        }

        // Apply "enters with counters" replacement effects from other battlefield permanents
        // (e.g., Gev: "Other creatures you control enter with additional +1/+1 counters")
        val (globalState, globalEvents) = EntersWithCountersHelper.applyGlobalEntersWithCounters(
            newState, entityId, controllerId
        )
        newState = globalState
        events.addAll(globalEvents)

        return newState to events
    }

    private fun resolveCounterType(filter: CounterTypeFilter): CounterType =
        EntersWithCountersHelper.resolveCounterType(filter)

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

        // Check if the spell can't be countered (tag component)
        if (container.has<CantBeCounteredComponent>()) {
            return ExecutionResult.success(state)
        }

        // Check if any permanent on the battlefield grants "can't be countered" to this spell
        if (isGrantedCantBeCountered(state, spellId)) {
            return ExecutionResult.success(state)
        }

        val spellComponent = container.get<SpellOnStackComponent>()
        val ownerId = cardComponent?.ownerId
            ?: spellComponent?.casterId
            ?: return ExecutionResult.error(state, "Cannot determine spell owner")

        // Remove from stack
        var newState = state.removeFromStack(spellId)

        // Put in graveyard (or exile if ExileAfterResolveComponent is present)
        val exileAfterResolve = container.has<ExileAfterResolveComponent>()
        val destZone = if (exileAfterResolve) Zone.EXILE else Zone.GRAVEYARD
        val destZoneKey = ZoneKey(ownerId, destZone)
        newState = newState.addToZone(destZoneKey, spellId)

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
                    destZone,
                    ownerId
                )
            )
        )
    }

    /**
     * Counter a spell on the stack and exile it instead of putting it into
     * its owner's graveyard. If the spell can't be countered, nothing happens.
     *
     * @param grantFreeCast If true, the controller of this effect may cast the
     *   exiled card without paying its mana cost for as long as it remains exiled.
     * @param controllerId The player who gains permission to cast the exiled card.
     * @return ExecutionResult with a boolean flag indicating if the spell was actually countered.
     */
    fun counterSpellToExile(
        state: GameState,
        spellId: EntityId,
        grantFreeCast: Boolean,
        controllerId: EntityId
    ): ExecutionResult {
        if (spellId !in state.stack) {
            return ExecutionResult.error(state, "Spell not on stack: $spellId")
        }

        val container = state.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Spell not found: $spellId")

        val cardComponent = container.get<CardComponent>()

        // Check if the spell can't be countered
        if (container.has<CantBeCounteredComponent>() || isGrantedCantBeCountered(state, spellId)) {
            return ExecutionResult.success(state)
        }

        val spellComponent = container.get<SpellOnStackComponent>()
        val ownerId = cardComponent?.ownerId
            ?: spellComponent?.casterId
            ?: return ExecutionResult.error(state, "Cannot determine spell owner")

        // Remove from stack
        var newState = state.removeFromStack(spellId)

        // Put in exile (instead of graveyard)
        val exileZone = ZoneKey(ownerId, Zone.EXILE)
        newState = newState.addToZone(exileZone, spellId)

        // Remove stack components and optionally grant free cast
        newState = newState.updateEntity(spellId) { c ->
            var updated = c.without<SpellOnStackComponent>().without<TargetsComponent>()
            if (grantFreeCast) {
                updated = updated
                    .with(MayPlayFromExileComponent(controllerId = controllerId, permanent = true))
                    .with(PlayWithoutPayingCostComponent(controllerId = controllerId, permanent = true))
            }
            updated
        }

        return ExecutionResult.success(
            newState,
            listOf(
                SpellCounteredEvent(spellId, cardComponent?.name ?: "Unknown"),
                ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    null,
                    Zone.EXILE,
                    ownerId
                )
            )
        )
    }

    /**
     * Counter an activated or triggered ability on the stack.
     * Unlike countering a spell, the ability is simply removed from the stack
     * without going to any zone (abilities are not cards).
     */
    fun counterAbility(state: GameState, abilityId: EntityId): ExecutionResult {
        if (abilityId !in state.stack) {
            return ExecutionResult.error(state, "Ability not on stack: $abilityId")
        }

        val container = state.getEntity(abilityId)
            ?: return ExecutionResult.error(state, "Ability not found: $abilityId")

        val description = container.get<TriggeredAbilityOnStackComponent>()?.description
            ?: container.get<ActivatedAbilityOnStackComponent>()?.let { "${it.sourceName}'s ability" }
            ?: "Unknown ability"

        // Remove from stack — abilities don't go to any zone
        val newState = state.removeFromStack(abilityId)

        return ExecutionResult.success(
            newState,
            listOf(AbilityCounteredEvent(abilityId, description))
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
        sourceId: EntityId? = null,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY
    ): List<ChosenTarget> {
        // Always project state for shroud/hexproof checks (Rule 702.18, 702.11)
        val projected = state.projectedState
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

                    // Check hexproof from color (Rule 702.11b)
                    if (entityController != controllerId) {
                        for (color in sourceColors) {
                            if (projected.hasKeyword(target.entityId, "HEXPROOF_FROM_${color.name}")) {
                                return@filterIndexed false
                            }
                        }
                    }

                    // Check can't-be-targeted-by-abilities (Shanna, Sisay's Legacy)
                    if (targetingSourceType != TargetingSourceType.SPELL && entityController != controllerId) {
                        val container = state.getEntity(target.entityId)
                        if (container?.has<CantBeTargetedByOpponentAbilitiesComponent>() == true) {
                            return@filterIndexed false
                        }
                    }

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
            is TargetObject -> requirement.filter
            else -> null
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Determine which zone a card is being cast from.
     */
    private fun findCastFromZone(
        state: GameState,
        cardId: EntityId,
        playerId: EntityId
    ): Zone? {
        val zones = listOf(Zone.HAND, Zone.GRAVEYARD, Zone.LIBRARY)
        for (zone in zones) {
            if (cardId in state.getZone(ZoneKey(playerId, zone))) {
                return zone
            }
        }
        // Check all players' exile zones (cards may be in another player's exile,
        // e.g., Villainous Wealth exiles from opponent's library)
        for (pid in state.turnOrder) {
            if (cardId in state.getZone(ZoneKey(pid, Zone.EXILE))) {
                return Zone.EXILE
            }
        }
        return null
    }

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

        // Check all players' exile zones (cards may be in another player's exile,
        // e.g., Villainous Wealth exiles from opponent's library)
        for (pid in state.turnOrder) {
            val exileZone = ZoneKey(pid, Zone.EXILE)
            if (cardId in state.getZone(exileZone)) {
                return state.removeFromZone(exileZone, cardId)
            }
        }

        // Check library (for Future Sight / play from top of library)
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        if (cardId in state.getZone(libraryZone)) {
            return state.removeFromZone(libraryZone, cardId)
        }

        return state
    }

    /**
     * Check if a spell on the stack is granted "can't be countered" by any permanent
     * on the battlefield with a GrantCantBeCountered static ability.
     */
    private fun isGrantedCantBeCountered(state: GameState, spellId: EntityId): Boolean {
        val spellOwner = state.getEntity(spellId)?.get<ControllerComponent>()?.playerId
            ?: return false
        val context = PredicateContext(controllerId = spellOwner)
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                val def = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                for (ability in def.staticAbilities) {
                    if (ability is GrantCantBeCountered) {
                        if (predicateEvaluator.matches(state, spellId, ability.filter, context)) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    // =========================================================================
    // Valiant / "first time targeted" tracking
    // =========================================================================

    /**
     * Check if the target entity has already been targeted by the given controller this turn.
     */
    private fun hasBeenTargetedByController(state: GameState, targetId: EntityId, controllerId: EntityId): Boolean {
        val component = state.getEntity(targetId)?.get<TargetedByControllerThisTurnComponent>()
        return component?.hasBeenTargetedBy(controllerId) == true
    }

    /**
     * Mark the target entity as having been targeted by the given controller this turn.
     */
    private fun markTargetedByController(state: GameState, targetId: EntityId, controllerId: EntityId): GameState {
        return state.updateEntity(targetId) { container ->
            val existing = container.get<TargetedByControllerThisTurnComponent>()
                ?: TargetedByControllerThisTurnComponent()
            container.with(existing.withController(controllerId))
        }
    }

    /**
     * Create the appropriate decision and continuation for an EntersWithChoice replacement effect.
     * Returns null if the choice cannot be presented (e.g., no creatures on battlefield for CREATURE_ON_BATTLEFIELD).
     */
    internal fun pauseForEntersWithChoice(
        state: GameState,
        spellId: EntityId,
        controllerId: EntityId,
        ownerId: EntityId,
        cardComponent: CardComponent,
        choice: EntersWithChoice
    ): ExecutionResult? {
        val chooserId = when (choice.chooser) {
            com.wingedsheep.sdk.scripting.references.Player.Opponent ->
                state.turnOrder.firstOrNull { it != controllerId } ?: controllerId
            else -> controllerId
        }

        return when (choice.choiceType) {
            ChoiceType.COLOR -> {
                val decisionId = "choose-color-enters-${spellId.value}"
                val decision = ChooseColorDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose a color",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    )
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.COLOR
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision)
            }

            ChoiceType.CREATURE_TYPE -> {
                val allCreatureTypes = com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES
                val decisionId = "choose-creature-type-enters-${spellId.value}"
                val decision = ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose a creature type",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = allCreatureTypes,
                    defaultSearch = ""
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.CREATURE_TYPE,
                    creatureTypes = allCreatureTypes
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision)
            }

            ChoiceType.CREATURE_ON_BATTLEFIELD -> {
                val battlefieldCreatures = state.getBattlefield().filter { entityId ->
                    val container = state.getEntity(entityId) ?: return@filter false
                    val card = container.get<CardComponent>() ?: return@filter false
                    val controller = container.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId ?: return@filter false
                    controller == controllerId && card.typeLine.isCreature && entityId != spellId
                }
                if (battlefieldCreatures.isEmpty()) return null // No creatures — enter without choice
                val decisionId = "choose-creature-enters-${spellId.value}"
                val decision = SelectCardsDecision(
                    id = decisionId,
                    playerId = controllerId,
                    prompt = "Choose another creature you control",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = battlefieldCreatures,
                    minSelections = 1,
                    maxSelections = 1,
                    useTargetingUI = true
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.CREATURE_ON_BATTLEFIELD
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision)
            }
        }
    }
}
