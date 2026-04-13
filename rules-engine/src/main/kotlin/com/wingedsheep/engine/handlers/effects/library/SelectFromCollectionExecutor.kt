package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
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
class SelectFromCollectionExecutor : EffectExecutor<SelectFromCollectionEffect> {

    override val effectType: KClass<SelectFromCollectionEffect> = SelectFromCollectionEffect::class

    private val amountEvaluator = DynamicAmountEvaluator()
    private val predicateEvaluator = PredicateEvaluator()

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
                predicateEvaluator.matches(state, cardId, effect.filter, predicateContext)
            }
        } else {
            cards
        }

        // Additionally filter by chosen creature type if requested
        if (effect.matchChosenCreatureType) {
            val chosenType = context.chosenCreatureType
            if (chosenType != null) {
                eligibleCards = eligibleCards.filter { cardId ->
                    val typeLine = state.getEntity(cardId)?.get<CardComponent>()?.typeLine
                    typeLine != null && typeLine.hasSubtype(Subtype(chosenType))
                }
            }
        }

        // Resolve who makes the decision
        val decidingPlayerId = when (effect.chooser) {
            Chooser.Controller -> null // null = default to controller in createDecision
            Chooser.Opponent -> context.opponentId
                ?: return EffectResult.error(state, "No opponent for Opponent chooser")
            Chooser.TargetPlayer -> context.targets.firstOrNull()?.let {
                TargetResolutionUtils.run { it.toEntityId() }
            } ?: return EffectResult.error(state, "No target player for TargetPlayer chooser")
            Chooser.TriggeringPlayer -> context.triggeringEntityId
                ?: return EffectResult.error(state, "No triggering player for TriggeringPlayer chooser")
        }

        // Restrictions can tighten the maximum number of selectable cards (e.g.,
        // OnePerCardType caps the max at the number of distinct card types present).
        // They are also propagated into the continuation for response-time normalization.
        val restrictionCeiling = restrictionCeiling(effect.restrictions, state, eligibleCards)

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
                if (count >= eligibleCards.size && effect.restrictions.isEmpty()) {
                    // Must select all eligible — no choice needed
                    val collections = mutableMapOf(effect.storeSelected to eligibleCards)
                    if (remainderName != null) {
                        collections[remainderName] = cards.filter { it !in eligibleCards }
                    }
                    EffectResult.success(state).copy(updatedCollections = collections)
                } else {
                    val clamped = minOf(count, eligibleCards.size)
                    createDecision(state, context, effect, eligibleCards, clamped, clamped, decidingPlayerId, allCards = cards)
                }
            }

            is SelectionMode.ChooseUpTo -> {
                val requested = amountEvaluator.evaluate(state, selection.count, context)
                val count = minOf(requested, restrictionCeiling)
                if (eligibleCards.isEmpty()) {
                    if (effect.showAllCards && cards.isNotEmpty()) {
                        // Show all cards even though none are selectable (e.g., "look at top 3, you may reveal a creature or land")
                        return createDecision(state, context, effect, emptyList(), 0, 0, decidingPlayerId, allCards = cards, nonSelectableCards = cards)
                    }
                    val collections = mutableMapOf(effect.storeSelected to emptyList<EntityId>())
                    if (remainderName != null) {
                        collections[remainderName] = cards
                    }
                    return EffectResult.success(state).copy(updatedCollections = collections)
                }
                val nonSelectable = if (effect.showAllCards) cards.filter { it !in eligibleCards } else emptyList()
                createDecision(state, context, effect, eligibleCards, 0, minOf(count, eligibleCards.size), decidingPlayerId, allCards = cards, nonSelectableCards = nonSelectable)
            }

            is SelectionMode.Random -> {
                // No player decision — engine randomly picks N cards
                val count = minOf(amountEvaluator.evaluate(state, selection.count, context), restrictionCeiling)
                val selected = eligibleCards.shuffled().take(count)
                val collections = mutableMapOf(effect.storeSelected to selected)
                if (remainderName != null) {
                    collections[remainderName] = cards.filter { it !in selected }
                }
                EffectResult.success(state).copy(updatedCollections = collections)
            }

            is SelectionMode.ChooseAnyNumber -> {
                // Player picks 0 to all eligible cards
                if (eligibleCards.isEmpty()) {
                    val collections = mutableMapOf(effect.storeSelected to emptyList<EntityId>())
                    if (remainderName != null) collections[remainderName] = cards
                    return EffectResult.success(state).copy(updatedCollections = collections)
                }
                val maxSelectable = minOf(eligibleCards.size, restrictionCeiling)
                createDecision(state, context, effect, eligibleCards, 0, maxSelectable, decidingPlayerId, allCards = cards)
            }
        }
    }

    /**
     * Compute the tightest upper bound on the number of cards that may be selected
     * given the active [restrictions]. Returns [Int.MAX_VALUE] when no restriction
     * narrows the bound.
     */
    private fun restrictionCeiling(
        restrictions: List<SelectionRestriction>,
        state: GameState,
        eligibleCards: List<EntityId>
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
        nonSelectableCards: List<EntityId> = emptyList()
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
                imageUri = null
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
            onePerCardType = effect.restrictions.any { it is SelectionRestriction.OnePerCardType }
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
}
