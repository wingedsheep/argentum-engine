package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for SelectFromCollectionEffect.
 *
 * Reads cards from a named collection in [EffectContext.storedCollections],
 * presents a selection decision to the player (or auto-selects), and stores
 * the selected and remainder collections.
 *
 * When a player decision is needed, pushes a [SelectFromCollectionContinuation]
 * and returns paused. The continuation handler splits the collections when
 * the player responds.
 */
class SelectFromCollectionExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<SelectFromCollectionEffect> {

    override val effectType: KClass<SelectFromCollectionEffect> = SelectFromCollectionEffect::class

    private val amountEvaluator = DynamicAmountEvaluator()
    private val predicateEvaluator = PredicateEvaluator()
    private val manaSolver by lazy { ManaSolver(cardRegistry) }

    override fun execute(
        state: GameState,
        effect: SelectFromCollectionEffect,
        context: EffectContext
    ): EffectResult {
        val cards = context.pipeline.storedCollections[effect.from]
            ?: return EffectResult.error(state, "No collection named '${effect.from}' in storedCollections")

        val remainderName = effect.storeRemainder

        if (cards.isEmpty()) {
            // Nothing to select from — store empty collections
            val collections = mutableMapOf(effect.storeSelected to emptyList<EntityId>())
            if (remainderName != null) {
                collections[remainderName] = emptyList()
            }
            return EffectResult.success(state).copy(updatedCollections = collections)
        }

        // Apply filter to narrow selectable cards (e.g., "creature card" for Animal Magnetism)
        var eligibleCards = if (effect.filter != GameObjectFilter.Any) {
            val predicateContext = PredicateContext.fromEffectContext(context)
            cards.filter { cardId ->
                predicateEvaluator.matches(state, state.projectedState, cardId, effect.filter, predicateContext)
            }
        } else {
            cards
        }

        // Additionally filter by chosen creature type if requested.
        // Cards with the Changeling keyword (Rule 702.73) have every creature type and
        // therefore match regardless of the chosen type — even outside the battlefield.
        if (effect.matchChosenCreatureType) {
            val chosenType = context.chosenCreatureType
            if (chosenType != null) {
                eligibleCards = eligibleCards.filter { cardId ->
                    val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return@filter false
                    Keyword.CHANGELING in cardComponent.baseKeywords ||
                        cardComponent.typeLine.hasSubtype(Subtype(chosenType))
                }
            }
        }

        // Resolve who makes the decision
        val decidingPlayerId = when (effect.chooser) {
            Chooser.Controller -> null // null = default to controller in createDecision
            Chooser.Opponent -> state.getOpponents(context.controllerId).firstOrNull()
                ?: return EffectResult.error(state, "No opponent for Opponent chooser")
            Chooser.TargetPlayer -> context.targets.firstOrNull()?.let {
                TargetResolutionUtils.run { it.toEntityId() }
            } ?: return EffectResult.error(state, "No target player for TargetPlayer chooser")
            Chooser.TriggeringPlayer -> context.triggeringEntityId
                ?: return EffectResult.error(state, "No triggering player for TriggeringPlayer chooser")
            Chooser.SourceController -> {
                val sourceId = context.sourceId
                    ?: return EffectResult.error(state, "No source entity for SourceController chooser")
                state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
                    ?: return EffectResult.error(state, "Source entity has no ControllerComponent for SourceController chooser")
            }
            Chooser.ControllerOfSelection -> {
                val deriveFrom = eligibleCards.firstOrNull() ?: cards.firstOrNull()
                    ?: return EffectResult.error(state, "No card to derive controller for ControllerOfSelection chooser")
                state.projectedState.getController(deriveFrom)
                    ?: state.getEntity(deriveFrom)?.get<ControllerComponent>()?.playerId
                    ?: return EffectResult.error(state, "Could not resolve controller for ControllerOfSelection chooser")
            }
            Chooser.ControllerOfTarget -> {
                val targetId = context.targets.firstOrNull()?.let {
                    TargetResolutionUtils.run { it.toEntityId() }
                } ?: return EffectResult.error(state, "No target for ControllerOfTarget chooser")
                // Controller of the targeted permanent; fall back to its owner once it has
                // left the battlefield (e.g. destroyed earlier in the same resolution).
                state.getEntity(targetId)?.get<ControllerComponent>()?.playerId
                    ?: state.getEntity(targetId)?.get<CardComponent>()?.ownerId
                    ?: return EffectResult.error(state, "Could not resolve controller for ControllerOfTarget chooser")
            }
        }

        // OnePerColor(matchControllerPermanentColors = true) narrows eligibility to
        // cards whose colours intersect the chooser's permanent colours.
        val chooserId = decidingPlayerId ?: context.controllerId
        val controllerPermanentColors: Set<com.wingedsheep.sdk.core.Color>? =
            if (effect.restrictions.any { it is SelectionRestriction.OnePerColor && it.matchControllerPermanentColors }) {
                colorsAmongPermanentsControlledBy(state, chooserId)
            } else null
        if (controllerPermanentColors != null) {
            eligibleCards = eligibleCards.filter { cardId ->
                val cardColors = state.getEntity(cardId)?.get<CardComponent>()?.colors ?: emptySet()
                cardColors.any { it in controllerPermanentColors }
            }
        }

        // Restrictions can tighten the maximum number of selectable cards (e.g.,
        // OnePerCardType caps the max at the number of distinct card types present).
        // They are also propagated into the continuation for response-time normalization.
        val restrictionCeiling = restrictionCeiling(effect.restrictions, state, context, eligibleCards, controllerPermanentColors)

        return when (val selection = effect.selection) {
            is SelectionMode.All -> {
                // Auto-select all eligible, no decision needed
                val collections = mutableMapOf(effect.storeSelected to eligibleCards)
                if (remainderName != null) {
                    collections[remainderName] = cards.filter { it !in eligibleCards }
                }
                EffectResult.success(state).copy(updatedCollections = collections)
            }

            is SelectionMode.ChooseExactly -> {
                val requested = amountEvaluator.evaluate(state, selection.count, context)
                val count = minOf(requested, restrictionCeiling)
                if (eligibleCards.isEmpty() || count == 0) {
                    // No eligible cards or zero requested — auto-select nothing
                    val collections = mutableMapOf(effect.storeSelected to emptyList<EntityId>())
                    if (remainderName != null) {
                        collections[remainderName] = cards
                    }
                    return EffectResult.success(state).copy(updatedCollections = collections)
                }
                if (count >= eligibleCards.size && effect.restrictions.isEmpty() && !effect.alwaysPrompt) {
                    // Must select all eligible — no choice needed
                    val collections = mutableMapOf(effect.storeSelected to eligibleCards)
                    if (remainderName != null) {
                        collections[remainderName] = cards.filter { it !in eligibleCards }
                    }
                    EffectResult.success(state).copy(updatedCollections = collections)
                } else {
                    val clamped = minOf(count, eligibleCards.size)
                    val nonSelectable = if (effect.showAllCards) cards.filter { it !in eligibleCards } else emptyList()
                    val conditionalMinimums = conditionalMinimumsFor(state, context, effect.restrictions, eligibleCards, clamped)
                    val minSelections = conditionalMinimums.minOfOrNull { it.minimumSelections } ?: clamped
                    createDecision(
                        state,
                        context,
                        effect,
                        eligibleCards,
                        minSelections,
                        clamped,
                        decidingPlayerId,
                        allCards = cards,
                        nonSelectableCards = nonSelectable,
                        controllerPermanentColors = controllerPermanentColors,
                        conditionalMinimums = conditionalMinimums
                    )
                }
            }

            is SelectionMode.ChooseUpTo -> {
                val requested = amountEvaluator.evaluate(state, selection.count, context)
                val count = minOf(requested, restrictionCeiling)
                if (eligibleCards.isEmpty()) {
                    if (effect.showAllCards && cards.isNotEmpty()) {
                        // Show all cards even though none are selectable (e.g., "look at top 3, you may reveal a creature or land")
                        return createDecision(state, context, effect, emptyList(), 0, 0, decidingPlayerId, allCards = cards, nonSelectableCards = cards, controllerPermanentColors = controllerPermanentColors)
                    }
                    val collections = mutableMapOf(effect.storeSelected to emptyList<EntityId>())
                    if (remainderName != null) {
                        collections[remainderName] = cards
                    }
                    return EffectResult.success(state).copy(updatedCollections = collections)
                }
                val nonSelectable = if (effect.showAllCards) cards.filter { it !in eligibleCards } else emptyList()
                createDecision(state, context, effect, eligibleCards, 0, minOf(count, eligibleCards.size), decidingPlayerId, allCards = cards, nonSelectableCards = nonSelectable, controllerPermanentColors = controllerPermanentColors)
            }

            is SelectionMode.Random -> {
                // No player decision — engine randomly picks N cards
                val count = minOf(amountEvaluator.evaluate(state, selection.count, context), restrictionCeiling)
                val (shuffledEligible, stateAfterShuffle) = state.nextRandom { shuffle(eligibleCards) }
                val selected = shuffledEligible.take(count)
                val collections = mutableMapOf(effect.storeSelected to selected)
                if (remainderName != null) {
                    collections[remainderName] = cards.filter { it !in selected }
                }
                EffectResult.success(stateAfterShuffle).copy(updatedCollections = collections)
            }

            is SelectionMode.ChooseAnyNumber -> {
                // Player picks 0 to all eligible cards
                if (eligibleCards.isEmpty()) {
                    if (effect.showAllCards && cards.isNotEmpty()) {
                        // Show all cards even though none are selectable (caster still sees the reveal).
                        return createDecision(state, context, effect, emptyList(), 0, 0, decidingPlayerId, allCards = cards, nonSelectableCards = cards, controllerPermanentColors = controllerPermanentColors)
                    }
                    val collections = mutableMapOf(effect.storeSelected to emptyList<EntityId>())
                    if (remainderName != null) collections[remainderName] = cards
                    return EffectResult.success(state).copy(updatedCollections = collections)
                }
                val maxSelectable = minOf(eligibleCards.size, restrictionCeiling)
                if (maxSelectable <= 0 && !effect.showAllCards) {
                    // A restriction ceiling of zero (e.g. MaxAffordablePayment with no mana
                    // available) makes the choice vacuous — skip the prompt, select nothing.
                    val collections = mutableMapOf(effect.storeSelected to emptyList<EntityId>())
                    if (remainderName != null) collections[remainderName] = cards
                    return EffectResult.success(state).copy(updatedCollections = collections)
                }
                val nonSelectable = if (effect.showAllCards) cards.filter { it !in eligibleCards } else emptyList()
                createDecision(state, context, effect, eligibleCards, 0, maxSelectable, decidingPlayerId, allCards = cards, nonSelectableCards = nonSelectable, controllerPermanentColors = controllerPermanentColors)
            }
        }
    }

    /**
     * Compute the tightest upper bound on the number of cards that may be selected
     * given the active [restrictions]. Returns [Int.MAX_VALUE] when no restriction
     * narrows the bound.
     */
    private fun colorsAmongPermanentsControlledBy(
        state: GameState,
        playerId: EntityId
    ): Set<com.wingedsheep.sdk.core.Color> {
        val colors = mutableSetOf<com.wingedsheep.sdk.core.Color>()
        for (entityId in state.getBattlefield()) {
            val controller = state.getEntity(entityId)?.get<ControllerComponent>()?.playerId
            if (controller != playerId) continue
            colors += state.getEntity(entityId)?.get<CardComponent>()?.colors ?: emptySet()
        }
        return colors
    }

    private fun restrictionCeiling(
        restrictions: List<SelectionRestriction>,
        state: GameState,
        context: EffectContext,
        eligibleCards: List<EntityId>,
        controllerPermanentColors: Set<com.wingedsheep.sdk.core.Color>? = null
    ): Int {
        if (restrictions.isEmpty()) return Int.MAX_VALUE
        var ceiling = Int.MAX_VALUE
        for (restriction in restrictions) {
            val limit = when (restriction) {
                is SelectionRestriction.OnePerCardType -> {
                    val distinctTypes = eligibleCards.flatMap { cardId ->
                        state.getEntity(cardId)?.get<CardComponent>()?.typeLine?.cardTypes ?: emptySet()
                    }.toSet()
                    distinctTypes.size.coerceAtLeast(0)
                }
                is SelectionRestriction.OnePerColor -> {
                    // Each colour contributes one slot. When matchControllerPermanentColors is
                    // set, eligibility is already filtered, so colourless cards are absent.
                    // Otherwise colourless cards are unconstrained and raise the ceiling
                    // by their full count.
                    val distinctColors = mutableSetOf<com.wingedsheep.sdk.core.Color>()
                    var colourlessCount = 0
                    for (cardId in eligibleCards) {
                        val cardColors = state.getEntity(cardId)?.get<CardComponent>()?.colors ?: emptySet()
                        if (cardColors.isEmpty()) colourlessCount++ else distinctColors += cardColors
                    }
                    if (restriction.matchControllerPermanentColors && controllerPermanentColors != null) {
                        // Slots are bounded by colours of controller's permanents, intersected with
                        // colours actually present in the eligible set.
                        distinctColors.intersect(controllerPermanentColors).size
                    } else {
                        distinctColors.size + colourlessCount
                    }
                }
                is SelectionRestriction.OnePerCardName -> {
                    eligibleCards.mapNotNull { cardId ->
                        state.getEntity(cardId)?.get<CardComponent>()?.name
                    }.toSet().size.coerceAtLeast(0)
                }
                is SelectionRestriction.TotalManaValueAtMost -> {
                    // Greedy upper bound: how many cards fit under the cap when picking
                    // the cheapest first. Any selection of more cards than this is
                    // unsatisfiable, since even the cheapest combination would exceed.
                    val sortedMvs = eligibleCards
                        .map { state.getEntity(it)?.get<CardComponent>()?.manaValue ?: 0 }
                        .sorted()
                    var running = 0
                    var picked = 0
                    for (mv in sortedMvs) {
                        if (running + mv > restriction.max) break
                        running += mv
                        picked++
                    }
                    picked
                }
                is SelectionRestriction.OnePerBasicLandType -> {
                    // One slot per distinct basic land type present among eligible lands.
                    // Typeless lands contribute nothing (they can't be kept).
                    eligibleCards.flatMap { cardId ->
                        state.getEntity(cardId)?.get<CardComponent>()?.typeLine?.subtypes
                            ?.filter { it.value in com.wingedsheep.sdk.core.Subtype.ALL_BASIC_LAND_TYPES }
                            ?: emptySet()
                    }.toSet().size
                }
                is SelectionRestriction.ReducedMinimumIfMatches -> Int.MAX_VALUE
                is SelectionRestriction.MaxAffordablePayment -> {
                    // Cap at what the payer could actually pay for: floor(available / per-card).
                    // Available mana counts floating mana plus untapped sources, matching the
                    // affordability pre-pass of the downstream Gate.MayPay.
                    val payerId = TargetResolutionUtils
                        .resolvePlayerTarget(EffectTarget.PlayerRef(restriction.payer), context, state)
                        ?: context.controllerId
                    val available = manaSolver.getAvailableManaCount(state, payerId)
                    if (restriction.manaPerSelected <= 0) Int.MAX_VALUE
                    else available / restriction.manaPerSelected
                }
            }
            if (limit < ceiling) ceiling = limit
        }
        return ceiling
    }

    /**
     * @param cards The cards presented as selectable options in the decision
     * @param allCards The full collection for remainder computation (defaults to [cards]).
     *   When a filter narrows the selectable options, allCards should contain all cards
     *   so that non-eligible cards are included in the remainder.
     */
    private fun createDecision(
        state: GameState,
        context: EffectContext,
        effect: SelectFromCollectionEffect,
        cards: List<EntityId>,
        minSelections: Int,
        maxSelections: Int,
        decidingPlayerId: EntityId? = null,
        allCards: List<EntityId> = cards,
        nonSelectableCards: List<EntityId> = emptyList(),
        controllerPermanentColors: Set<com.wingedsheep.sdk.core.Color>? = null,
        conditionalMinimums: List<ConditionalSelectionMinimum> = emptyList()
    ): EffectResult {
        val playerId = decidingPlayerId ?: context.controllerId
        val decisionId = UUID.randomUUID().toString()
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // Build card info for hidden-zone cards (library cards are normally hidden)
        val allDisplayCards = cards + nonSelectableCards
        val cardInfoMap = allDisplayCards.associateWith { cardId ->
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = null,
                colors = cardComponent?.colors?.map { it.name }?.toList() ?: emptyList()
            )
        }

        val prompt = effect.prompt ?: when {
            minSelections == maxSelections -> "Choose $minSelections card${if (minSelections != 1) "s" else ""}"
            minSelections == 0 -> "Choose up to $maxSelections card${if (maxSelections != 1) "s" else ""}"
            else -> "Choose $minSelections to $maxSelections cards"
        }

        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = playerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = cards,
            minSelections = minSelections,
            maxSelections = maxSelections,
            ordered = false,
            cardInfo = cardInfoMap,
            selectedLabel = effect.selectedLabel,
            remainderLabel = effect.remainderLabel,
            useTargetingUI = effect.useTargetingUI,
            nonSelectableOptions = nonSelectableCards,
            onePerCardType = effect.restrictions.any { it is SelectionRestriction.OnePerCardType },
            onePerColor = effect.restrictions.any { it is SelectionRestriction.OnePerColor },
            availableColors = controllerPermanentColors?.map { it.name },
            onePerCardName = effect.restrictions.any { it is SelectionRestriction.OnePerCardName },
            onePerBasicLandType = effect.restrictions.any { it is SelectionRestriction.OnePerBasicLandType },
            maxTotalManaValue = effect.restrictions
                .filterIsInstance<SelectionRestriction.TotalManaValueAtMost>()
                .also {
                    require(it.size <= 1) {
                        "SelectFromCollectionEffect has multiple TotalManaValueAtMost restrictions; " +
                            "compose a single cap instead."
                    }
                }
                .singleOrNull()?.max,
            conditionalMinimums = conditionalMinimums
        )

        val continuation = SelectFromCollectionContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            allCards = allCards,
            storeSelected = effect.storeSelected,
            storeRemainder = effect.storeRemainder,
            storedCollections = context.pipeline.storedCollections,
            restrictions = effect.restrictions
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    private fun conditionalMinimumsFor(
        state: GameState,
        context: EffectContext,
        restrictions: List<SelectionRestriction>,
        eligibleCards: List<EntityId>,
        requiredSelections: Int
    ): List<ConditionalSelectionMinimum> {
        if (requiredSelections <= 0) return emptyList()
        val predicateContext = PredicateContext.fromEffectContext(context)
        return restrictions.mapNotNull { restriction ->
            val reduced = restriction as? SelectionRestriction.ReducedMinimumIfMatches ?: return@mapNotNull null
            if (reduced.reducedMinimum >= requiredSelections) return@mapNotNull null
            val matchingOptions = eligibleCards.filter { cardId ->
                predicateEvaluator.matches(state, state.projectedState, cardId, reduced.filter, predicateContext)
            }
            if (matchingOptions.size < reduced.requiredMatches) return@mapNotNull null
            ConditionalSelectionMinimum(
                requiredSelections = requiredSelections,
                minimumSelections = reduced.reducedMinimum,
                matchingOptions = matchingOptions,
                requiredMatches = reduced.requiredMatches,
                description = reduced.description
            )
        }
    }
}
