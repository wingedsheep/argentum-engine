package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.ChooserResolution
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.CompelledActionImmunity
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.LifePaymentPurpose
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.CompelledAction
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
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
    private val cardRegistry: CardRegistry,
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<SelectFromCollectionEffect> {
    private val amountEvaluator = predicateEvaluator.amounts

    override val effectType: KClass<SelectFromCollectionEffect> = SelectFromCollectionEffect::class

    private val manaSolver by lazy { ManaSolver(cardRegistry, predicateEvaluator) }

    private val visibility = com.wingedsheep.engine.view.Visibility(cardRegistry, conditionEvaluator = predicateEvaluator.conditions)

    /**
     * Whether this selection only picks the cards a later step of the running program discards
     * (a `MoveCollection` of [SelectFromCollectionEffect.storeSelected] with [MoveType.Discard]),
     * for a spell or ability every owner of [cards] is protected from ([CompelledActionImmunity]).
     * The running program is the frame the interpreter pushed beneath this leaf.
     */
    private fun selectionFeedsForbiddenDiscard(
        state: GameState,
        effect: SelectFromCollectionEffect,
        context: EffectContext,
        cards: List<EntityId>
    ): Boolean {
        if (context.discardIsCost || cards.isEmpty()) return false
        val cause = context.effectControllerId ?: context.controllerId
        val owners = cards.mapTo(HashSet()) { cardId ->
            state.getEntity(cardId)?.get<com.wingedsheep.engine.state.components.identity.OwnerComponent>()?.playerId
                ?: state.getEntity(cardId)?.get<CardComponent>()?.ownerId ?: return false
        }
        if (!owners.all { CompelledActionImmunity.appliesTo(state, it, cause, CompelledAction.DISCARD, predicateEvaluator) }) return false
        if (!cards.all { cardId -> owners.any { cardId in state.getHand(it) } }) return false
        val program = state.peekContinuation() as? EffectProgram ?: return false
        val steps = program.cursor.steps.filterIsInstance<CursorStep.Child>()
        if (steps.size != program.cursor.steps.size || steps.isEmpty()) return false
        var composite = program.root as? CompositeEffect ?: return false
        for (step in steps.dropLast(1)) {
            composite = composite.effects.getOrNull(step.index) as? CompositeEffect ?: return false
        }
        val index = steps.last().index
        if (composite.effects.getOrNull(index) != effect) return false
        return composite.effects.drop(index + 1).any {
            it is MoveCollectionEffect && it.from == effect.storeSelected && it.moveType == MoveType.Discard
        }
    }

    override fun execute(
        state: GameState,
        effect: SelectFromCollectionEffect,
        context: EffectContext
    ): EffectResult {
        val original = effect
        // Resolve any dynamic selection restriction (e.g. TotalManaValueAtMost bound to X on a
        // pay-{X} reflexive) to a concrete integer cap up front, so the ceiling calc, the
        // decision, the continuation, and response normalization all see a fixed value.
        @Suppress("NAME_SHADOWING")
        val effect = effect.resolveDynamicRestrictions(state, context)
        val cards = context.pipeline.storedCollections[effect.from]
            ?: return EffectResult.error(state, "No collection named '${effect.from}' in storedCollections")

        val remainderName = effect.storeRemainder
        val unknown = context.pipeline.storedCollections[
            com.wingedsheep.engine.handlers.effects.EffectDiscardDestinations.UNDEFINED + ":" + effect.from
        ].orEmpty().toSet()
        val readsCharacteristics = effect.matchChosenCreatureType || effect.restrictions.any {
            when (it) {
                is SelectionRestriction.MaxAffordablePayment, is SelectionRestriction.ReducedMinimumIfMatches -> false
                is SelectionRestriction.OnePerCardType, is SelectionRestriction.OnePerColor,
                is SelectionRestriction.OnePerCardName, is SelectionRestriction.OnePerPower,
                is SelectionRestriction.TotalManaValueAtMost, is SelectionRestriction.TotalPowerAtMost,
                is SelectionRestriction.OnePerBasicLandType -> true
            }
        }
        val characteristicEligible = if (readsCharacteristics) cards.filterNot { it in unknown } else cards

        // Every player sees whether a question is pending, so the question is skipped, or answered
        // here, only on facts every player has (#3340). A pool whose membership hidden cards
        // decided — a search, a filtered look at a hand — is always asked about, so an empty
        // answer ("fail to find", CR 701.23b) is a real one rather than a skipped question.
        val poolDeciders = HiddenPools.deciders(context.pipeline.storedCollections, effect.from)

        if (cards.isEmpty() && poolDeciders.isEmpty()) {
            // Nothing to select from, and everyone can see that — store empty collections
            val collections = mutableMapOf(effect.storeSelected to emptyList<EntityId>())
            if (remainderName != null) {
                collections[remainderName] = emptyList()
            }
            return EffectResult.success(state).copy(updatedCollections = collections)
        }

        // The chosen cards are about to be discarded by an opponent's spell or ability, and every
        // player whose hand they'd come from can't be made to discard (Tamiyo, Collector of Tales):
        // the discard won't happen, so the question isn't asked. Both facts are public.
        if (selectionFeedsForbiddenDiscard(state, original, context, cards)) {
            val collections = mutableMapOf(effect.storeSelected to emptyList<EntityId>())
            if (remainderName != null) collections[remainderName] = cards
            return EffectResult.success(state).copy(updatedCollections = collections)
        }

        if (effect.selection == SelectionMode.ChooseSpell) {
            require(effect.restrictions.isEmpty() && !effect.matchChosenCreatureType) {
                "ChooseSpell expresses eligibility through its face filter, not card-selection restrictions"
            }
            val faces = spellFaces(state, cards.filterNot { it in unknown }, effect, context)
            val chooser = when (val outcome = ChooserResolution.resolve(state, effect.chooser, context, cards)) {
                is ChooserResolution.Outcome.Resolved -> outcome.playerId
                is ChooserResolution.Outcome.NeedsOpponentPick -> return ChooserResolution.pauseForOpponentPick(
                    state, outcome.opponents, effect, context, prompt = "Choose which opponent chooses the spell"
                )
                is ChooserResolution.Outcome.Unresolvable -> return EffectResult.error(state, outcome.reason)
            }
            return createDecision(
                state, context, effect, faces.keys.toList(), 0, minOf(1, faces.size), decidingPlayerId = chooser,
                allCards = cards, nonSelectableCards = if (effect.showAllCards) cards - faces.keys else emptyList(),
                spellFaces = faces,
                hiddenDeciders = (poolDeciders + HiddenPools.hiddenOwners(state, cards, visibility, context.pipeline.storedCollections)).distinct()
            )
        }

        // Apply filter to narrow selectable cards (e.g., "creature card" for Animal Magnetism)
        var eligibleCards = if (effect.filter != GameObjectFilter.Any) {
            val predicateContext = PredicateContext.fromEffectContext(context)
            characteristicEligible.filter { cardId ->
                val filter = if (cardId in unknown) com.wingedsheep.engine.handlers.effects.EffectDiscardDestinations
                    .filterForUndefinedCharacteristics(effect.filter) ?: return@filter false else effect.filter
                predicateEvaluator.matches(state, state.projectedState, cardId, filter, predicateContext)
            }
        } else {
            characteristicEligible
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
                        cardComponent.typeLine.hasSubtype(Subtype(chosenType)) ||
                        // Cross-zone "is the chosen type" grants (Conspiracy / Leyline of
                        // Transformation) make non-battlefield creature cards count as the type.
                        state.projectedState.crossZoneGrantedSubtypes(state, cardId)
                            .any { it.equals(chosenType, ignoreCase = true) }
                }
            }
        }

        // Resolve who makes the decision. `null` = default to the controller in createDecision.
        val decidingPlayerId = when (
            val outcome = ChooserResolution.resolve(
                state,
                effect.chooser,
                context,
                selectionCards = eligibleCards.ifEmpty { cards }
            )
        ) {
            is ChooserResolution.Outcome.Resolved ->
                if (effect.chooser == Chooser.Controller) null else outcome.playerId
            // "An opponent chooses…" with several opponents: the controller says which one, then
            // this same effect runs again and presents the selection to them.
            is ChooserResolution.Outcome.NeedsOpponentPick -> return ChooserResolution.pauseForOpponentPick(
                state, outcome.opponents, effect, context,
                prompt = "Choose which opponent makes this choice"
            )
            // A chooser read off the cards ("that card's controller") has nobody to ask once the
            // pool is empty; nothing is selected, as before hidden pools were asked about.
            is ChooserResolution.Outcome.Unresolvable -> if (cards.isEmpty()) {
                return EffectResult.success(state).copy(
                    updatedCollections = listOfNotNull(effect.storeSelected, remainderName).associateWith { emptyList() }
                )
            } else {
                return EffectResult.error(state, "SelectFromCollection chooser: ${outcome.reason}")
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
                val cardColors = state.projectedState.colorSetOf(cardId)
                cardColors.any { it in controllerPermanentColors }
            }
        }

        // Restrictions can tighten the maximum number of selectable cards (e.g.,
        // OnePerCardType caps the max at the most cards that can each claim a distinct type).
        // They are also propagated into the continuation for response-time normalization.
        val restrictionCeiling = restrictionCeiling(effect.restrictions, state, context, eligibleCards, controllerPermanentColors)

        // Which cards are eligible is hidden when the pool was built from hidden cards, or when
        // this step reads the characteristics of a card some player can't see. Either way, how
        // many are eligible must not decide whether the chooser is asked.
        val readsCards = effect.filter != GameObjectFilter.Any || effect.matchChosenCreatureType ||
            readsCharacteristics || controllerPermanentColors != null
        val hiddenDeciders = if (readsCards) {
            (poolDeciders + HiddenPools.hiddenOwners(state, cards, visibility, context.pipeline.storedCollections)).distinct()
        } else {
            poolDeciders
        }
        val eligibilityHidden = hiddenDeciders.isNotEmpty()
        val prior = context.pipeline.storedCollections
        fun selectedWithoutAsking(selected: List<EntityId>, stateAfter: GameState = state): EffectResult {
            val collections = mutableMapOf(effect.storeSelected to selected)
            if (remainderName != null) collections[remainderName] = cards.filter { it !in selected }
            collections += HiddenPools.marks(listOfNotNull(effect.storeSelected, remainderName), hiddenDeciders, prior)
            collections += LibrarySearchControl.narrowPool(prior, effect.from, cards, selected)
            return EffectResult.success(stateAfter).copy(updatedCollections = collections)
        }
        // Nothing the chooser could pick, asked anyway: the empty selection is the only answer, so
        // the decision is forced and auto-answers. A look ([SelectFromCollectionEffect.showAllCards])
        // keeps showing the cards, as it always has.
        fun askWithNothingEligible(): EffectResult =
            if (effect.showAllCards && cards.isNotEmpty()) {
                createDecision(state, context, effect, emptyList(), 0, 0, decidingPlayerId, allCards = cards, nonSelectableCards = cards, controllerPermanentColors = controllerPermanentColors, hiddenDeciders = hiddenDeciders)
            } else {
                createDecision(state, context, effect, emptyList(), 0, 0, decidingPlayerId, allCards = cards, hiddenDeciders = hiddenDeciders)
            }

        return when (val selection = effect.selection) {
            SelectionMode.ChooseSpell -> error("Spell selection is handled before card filtering")
            // Auto-select all eligible: no decision is ever asked, so none can leak.
            is SelectionMode.All -> selectedWithoutAsking(eligibleCards)

            is SelectionMode.ChooseExactly -> {
                val requested = amountEvaluator.evaluate(state, selection.count, context)
                val count = minOf(requested, restrictionCeiling)
                if (count == 0 || (eligibleCards.isEmpty() && !eligibilityHidden)) {
                    // Zero requested, or visibly nothing eligible — auto-select nothing
                    return selectedWithoutAsking(emptyList())
                }
                if (eligibleCards.isEmpty()) return askWithNothingEligible()
                if (count >= eligibleCards.size && effect.restrictions.isEmpty() && !effect.alwaysPrompt && !eligibilityHidden) {
                    // Must select all eligible, and everyone can see so — no choice needed. Over
                    // hidden cards it is asked: selecting every option is then the only answer,
                    // which auto-answers as forced.
                    selectedWithoutAsking(eligibleCards)
                } else {
                    val clamped = minOf(count, eligibleCards.size)
                    val nonSelectable = if (effect.showAllCards) cards.filter { it !in eligibleCards } else emptyList()
                    val conditionalMinimums = conditionalMinimumsFor(state, context, effect.from, effect.restrictions, eligibleCards, clamped)
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
                        conditionalMinimums = conditionalMinimums,
                        hiddenDeciders = hiddenDeciders
                    )
                }
            }

            is SelectionMode.ChooseUpTo -> {
                val requested = amountEvaluator.evaluate(state, selection.count, context)
                val count = minOf(requested, restrictionCeiling)
                if (count <= 0 && !effect.showAllCards) {
                    // "Up to zero" is no choice at all — a dynamic count that came out 0 (Hurkyl,
                    // Master Wizard's per-type pick for a type you didn't cast) selects nothing.
                    return selectedWithoutAsking(emptyList())
                }
                if (eligibleCards.isEmpty()) {
                    // Show all cards even though none are selectable (e.g., "look at top 3, you may
                    // reveal a creature or land"); over a hidden pool, ask with nothing to pick.
                    if ((effect.showAllCards && cards.isNotEmpty()) || eligibilityHidden) return askWithNothingEligible()
                    return selectedWithoutAsking(emptyList())
                }
                val nonSelectable = if (effect.showAllCards) cards.filter { it !in eligibleCards } else emptyList()
                createDecision(state, context, effect, eligibleCards, 0, minOf(count, eligibleCards.size), decidingPlayerId, allCards = cards, nonSelectableCards = nonSelectable, controllerPermanentColors = controllerPermanentColors, hiddenDeciders = hiddenDeciders)
            }

            is SelectionMode.Random -> {
                // No player decision — engine randomly picks N cards
                val count = minOf(amountEvaluator.evaluate(state, selection.count, context), restrictionCeiling)
                val (shuffledEligible, stateAfterShuffle) = state.nextRandom { shuffle(eligibleCards) }
                selectedWithoutAsking(shuffledEligible.take(count), stateAfterShuffle)
            }

            is SelectionMode.ChooseAnyNumber -> {
                // Player picks 0 to all eligible cards
                if (eligibleCards.isEmpty()) {
                    // Show all cards even though none are selectable (caster still sees the
                    // reveal); over a hidden pool, ask with nothing to pick.
                    if ((effect.showAllCards && cards.isNotEmpty()) || eligibilityHidden) return askWithNothingEligible()
                    return selectedWithoutAsking(emptyList())
                }
                val maxSelectable = minOf(eligibleCards.size, restrictionCeiling)
                if (maxSelectable <= 0 && !effect.showAllCards) {
                    // A restriction ceiling of zero (e.g. MaxAffordablePayment with no mana
                    // available) makes the choice vacuous — skip the prompt, select nothing.
                    return selectedWithoutAsking(emptyList())
                }
                val nonSelectable = if (effect.showAllCards) cards.filter { it !in eligibleCards } else emptyList()
                createDecision(state, context, effect, eligibleCards, 0, maxSelectable, decidingPlayerId, allCards = cards, nonSelectableCards = nonSelectable, controllerPermanentColors = controllerPermanentColors, hiddenDeciders = hiddenDeciders)
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
            if (state.projectedState.getController(entityId) != playerId) continue
            colors += state.projectedState.colorSetOf(entityId)
        }
        return colors
    }

    /**
     * Replace any dynamic-capped [SelectionRestriction.TotalManaValueAtMost] (one carrying a
     * [SelectionRestriction.TotalManaValueAtMost.maxAmount], e.g. `DynamicAmount.XValue`) with a
     * fixed-integer cap resolved against the current context. No-op when nothing is dynamic, so
     * the common case allocates nothing.
     */
    private fun SelectFromCollectionEffect.resolveDynamicRestrictions(
        state: GameState,
        context: EffectContext
    ): SelectFromCollectionEffect {
        if (restrictions.none {
                it is SelectionRestriction.TotalManaValueAtMost && it.maxAmount != null
            }
        ) {
            return this
        }
        return copy(
            restrictions = restrictions.map { restriction ->
                val dynamic = (restriction as? SelectionRestriction.TotalManaValueAtMost)?.maxAmount
                if (dynamic != null) {
                    SelectionRestriction.TotalManaValueAtMost(
                        max = amountEvaluator.evaluate(state, dynamic, context)
                    )
                } else {
                    restriction
                }
            }
        )
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
                is SelectionRestriction.OnePerCardType ->
                    // Each card fills one type slot, so the cap is a maximum matching of cards
                    // to their types — not the count of types present.
                    com.wingedsheep.engine.mechanics.targeting.OnePerCardType.maxDistinctAssignment(
                        eligibleCards.map { com.wingedsheep.engine.mechanics.targeting.OnePerCardType.cardTypesOf(state, it) }
                    )
                is SelectionRestriction.OnePerColor -> {
                    // Each colour contributes one slot. When matchControllerPermanentColors is
                    // set, eligibility is already filtered, so colourless cards are absent.
                    // Otherwise colourless cards are unconstrained and raise the ceiling
                    // by their full count.
                    val distinctColors = mutableSetOf<com.wingedsheep.sdk.core.Color>()
                    var colourlessCount = 0
                    for (cardId in eligibleCards) {
                        val cardColors = state.projectedState.colorSetOf(cardId)
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
                is SelectionRestriction.OnePerPower -> {
                    // One slot per distinct fixed power present. Cards with no fixed power
                    // (mapNotNull drops them) can't be kept and so add no slots.
                    eligibleCards.mapNotNull { cardId ->
                        state.getEntity(cardId)?.get<CardComponent>()?.baseStats?.basePower
                    }.toSet().size
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
                is SelectionRestriction.TotalPowerAtMost -> {
                    // Greedy upper bound on the count: pick the lowest projected powers
                    // first. Any selection of more cards than this is unsatisfiable. Uses
                    // projected power (CLAUDE.md: battlefield P/T must read projection).
                    val sortedPowers = eligibleCards
                        .map { state.projectedState.getPower(it) ?: 0 }
                        .sorted()
                    var running = 0
                    var picked = 0
                    for (p in sortedPowers) {
                        if (running + p > restriction.max) break
                        running += p
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
                    // affordability pre-pass of the downstream Gate.MayPay. The life axis caps at
                    // floor(life / per-card) — payable down to 0 (CR 119.4), never while the
                    // payer can't pay life (CR 119.8).
                    val payerId = TargetResolutionUtils
                        .resolvePlayerTarget(EffectTarget.PlayerRef(restriction.payer), context, state)
                        ?: context.controllerId
                    val manaCap = if (restriction.manaPerSelected <= 0) Int.MAX_VALUE
                        else manaSolver.getAvailableManaCount(state, payerId) / restriction.manaPerSelected
                    val lifeCap = when {
                        restriction.lifePerSelected <= 0 -> Int.MAX_VALUE
                        state.isLifePaymentBlocked(payerId, LifePaymentPurpose.OTHER) -> 0
                        else -> maxOf(0, state.lifeTotal(payerId)) / restriction.lifePerSelected
                    }
                    minOf(manaCap, lifeCap)
                }
            }
            if (limit < ceiling) ceiling = limit
        }
        return ceiling
    }

    private fun spellFaces(
        state: GameState, cards: List<EntityId>, effect: SelectFromCollectionEffect, context: EffectContext
    ): Map<EntityId, List<Int>> = buildMap {
        val predicateContext = PredicateContext.fromEffectContext(context)
        for (id in cards) {
            if (id in state.getBattlefield()) continue
            val card = state.getEntity(id)?.get<CardComponent>() ?: continue
            val definition = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val faces = buildList {
                // A split card's combined characteristics are not a castable face.
                if (!definition.isSplit) add(-1 to card)
                if (id in state.getHand(context.controllerId) && com.wingedsheep.engine.mechanics.SplitSpellCasts.hasFuse(definition)) {
                    val fused = com.wingedsheep.engine.mechanics.SplitSpellCasts.fusedDefinition(definition)
                    add(com.wingedsheep.engine.mechanics.SplitSpellCasts.FUSED_FACE to card.copy(
                        manaCost = fused.manaCost, typeLine = fused.typeLine, colors = fused.colors,
                        oracleText = fused.oracleText, baseKeywords = fused.keywords))
                }
                if (definition.layout == com.wingedsheep.sdk.model.CardLayout.MODAL_DFC) {
                    definition.backFace?.let { face ->
                        add(-2 to card.copy(
                            name = face.name, typeLine = face.typeLine, manaCost = face.manaCost,
                            oracleText = face.oracleText, baseStats = face.creatureStats,
                            baseKeywords = face.keywords, colors = face.colors
                        ))
                    }
                }
                if (!definition.isPreparation) definition.cardFaces.forEachIndexed { index, face ->
                    add(index to card.copy(
                        name = face.name, typeLine = face.typeLine, manaCost = face.manaCost,
                        oracleText = face.oracleText, baseKeywords = face.keywords, colors = face.manaCost.colors
                    ))
                }
            }.filter { (_, face) -> !face.typeLine.isLand }.filter { (_, face) ->
                val candidateState = if (face === card) state else state.updateEntity(id) { it.with(face) }
                // The candidate is outside the battlefield; its face overlay changes no projected permanent.
                predicateEvaluator.matches(candidateState, state.projectedState, id, effect.filter, predicateContext)
            }.map { it.first }
            if (faces.isNotEmpty()) put(id, faces)
        }
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
        conditionalMinimums: List<ConditionalSelectionMinimum> = emptyList(),
        spellFaces: Map<EntityId, List<Int>>? = null,
        hiddenDeciders: List<EntityId> = emptyList()
    ): EffectResult {
        // A searcher someone else controls while they search (Opposition Agent) doesn't make the pick.
        val playerId = LibrarySearchControl.decider(
            context.pipeline.storedCollections, effect.from, decidingPlayerId ?: context.controllerId
        )
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
                colors = if (cardComponent == null) emptyList() else state.projectedState.colorsOf(cardId).toList(),
                power = cardComponent?.baseStats?.basePower,
                zone = state.logicalZone(cardId)?.zoneType
            )
        }

        // A choice among cards outside the game (a wish, Learn) says so: the cards on offer come
        // from a zone the player never otherwise sees, and "Choose up to 1 card" alone reads as if
        // they were drawn from nowhere.
        val fromOutside = cards.isNotEmpty() &&
            cards.all { state.logicalZone(it)?.zoneType == com.wingedsheep.sdk.core.Zone.SIDEBOARD }
        val where = if (fromOutside) " from outside the game" else ""
        // `{counter}` names the counter kind a `CounterKindsAmong` loop is visiting ("Choose a
        // token to get a {counter} counter" — Exotic Pets), so each per-kind pick says which.
        val prompt = effect.prompt?.let { template ->
            context.chosenCounterType?.let { template.replace("{counter}", it.printed) } ?: template
        } ?: when {
            minSelections == maxSelections -> "Choose $minSelections card${if (minSelections != 1) "s" else ""}$where"
            minSelections == 0 -> "Choose up to $maxSelections card${if (maxSelections != 1) "s" else ""}$where"
            else -> "Choose $minSelections to $maxSelections cards$where"
        }

        val librarySearch = context.pipeline.storedCollections[PipelineState.searchedKey(effect.from)]
            .orEmpty().isNotEmpty()
        // The controlling player picks for the searcher, and what they find is exiled for the picker.
        val controlledSearch = playerId != (decidingPlayerId ?: context.controllerId)

        val decision = { decisionId: String -> SelectCardsDecision(
            id = decisionId,
            playerId = playerId,
            prompt = if (controlledSearch) {
                // The authored prompt speaks to the searcher, and names a destination the cards
                // won't reach: say whose search it is and where the found cards go instead.
                val what = prompt.replace(Regex("\\byour\\b"), "their").replace(Regex(" to put .*$"), "")
                "You control your opponent's search. $what — the cards found are exiled, and you may play them"
            } else prompt,
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
            selectedLabel = if (controlledSearch) "Exile — you may play it" else effect.selectedLabel,
            remainderLabel = effect.remainderLabel,
            useTargetingUI = effect.useTargetingUI,
            nonSelectableOptions = nonSelectableCards,
            onePerCardType = effect.restrictions.any { it is SelectionRestriction.OnePerCardType },
            onePerColor = effect.restrictions.any { it is SelectionRestriction.OnePerColor },
            availableColors = controllerPermanentColors?.map { it.name },
            onePerCardName = effect.restrictions.any { it is SelectionRestriction.OnePerCardName },
            onePerBasicLandType = effect.restrictions.any { it is SelectionRestriction.OnePerBasicLandType },
            onePerPower = effect.restrictions.any { it is SelectionRestriction.OnePerPower },
            maxTotalManaValue = effect.restrictions
                .filterIsInstance<SelectionRestriction.TotalManaValueAtMost>()
                .also {
                    require(it.size <= 1) {
                        "SelectFromCollectionEffect has multiple TotalManaValueAtMost restrictions; " +
                            "compose a single cap instead."
                    }
                }
                .singleOrNull()?.max,
            maxTotalPower = effect.restrictions
                .filterIsInstance<SelectionRestriction.TotalPowerAtMost>()
                .also {
                    require(it.size <= 1) {
                        "SelectFromCollectionEffect has multiple TotalPowerAtMost restrictions; " +
                            "compose a single cap instead."
                    }
                }
                .singleOrNull()?.max,
            conditionalMinimums = conditionalMinimums,
            librarySearch = librarySearch,
            declineLabel = if (librarySearch && minSelections == 0) "Fail to find" else null
        ) }

        val continuation = SelectFromCollectionContinuation(
            playerId = playerId,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences,
            sourceName = sourceName,
            allCards = allCards,
            sourceCollection = effect.from,
            storeSelected = effect.storeSelected,
            storeRemainder = effect.storeRemainder,
            storedCollections = context.pipeline.storedCollections,
            restrictions = effect.restrictions,
            spellFaces = spellFaces,
            shownCards = allDisplayCards,
            // Only a fixed count leaves the answer's size to the hidden cards: "choose up to" and
            // "any number" sizes are the chooser's own choice, which a later skip may follow.
            hiddenPoolDeciders = if (effect.selection is SelectionMode.ChooseExactly) hiddenDeciders else emptyList()
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation))
    }

    private fun conditionalMinimumsFor(
        state: GameState,
        context: EffectContext,
        contextSelectionCollection: String,
        restrictions: List<SelectionRestriction>,
        eligibleCards: List<EntityId>,
        requiredSelections: Int
    ): List<ConditionalSelectionMinimum> {
        if (requiredSelections <= 0) return emptyList()
        val predicateContext = PredicateContext.fromEffectContext(context)
        return restrictions.mapNotNull { restriction ->
            val reduced = restriction as? SelectionRestriction.ReducedMinimumIfMatches ?: return@mapNotNull null
            if (reduced.reducedMinimum >= requiredSelections) return@mapNotNull null
            val unknown = context.pipeline.storedCollections[
                com.wingedsheep.engine.handlers.effects.EffectDiscardDestinations.UNDEFINED + ":" + contextSelectionCollection
            ].orEmpty().toSet()
            val matchingOptions = eligibleCards.filter { cardId ->
                val filter = if (cardId in unknown) com.wingedsheep.engine.handlers.effects.EffectDiscardDestinations
                    .filterForUndefinedCharacteristics(reduced.filter) ?: return@filter false else reduced.filter
                predicateEvaluator.matches(state, state.projectedState, cardId, filter, predicateContext)
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
