package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.targets.*

/**
 * Resolves a permanent spell (CR 608.3): asks the "as this enters" questions its replacement effects
 * pose (CR 614.12) — pausing for each — and then puts it onto the battlefield through [PermanentEntry].
 */
internal class PermanentSpellResolver(
    private val cardRegistry: CardRegistry,
    private val effects: EffectExecutorRegistry,
    private val predicateEvaluator: PredicateEvaluator,
    private val permanentEntry: PermanentEntry,
    private val entersWithChoicePrompt: EntersWithChoicePrompt
) {
    private val amountEvaluator = predicateEvaluator.amounts
    /**
     * Resolve a permanent spell - put it on the battlefield.
     * May pause for player input (e.g., Clone choosing a creature to copy).
     *
     * Before the permanent enters, its "as this enters" replacement effects (CR 614.12) each ask
     * their question in turn — enters as a copy, the enters-with choices, amplify, exile-for-counters,
     * devour, then "pay life or enter tapped". The first one that has something to ask pauses
     * resolution; its continuation resumer completes the entry.
     */
    fun resolvePermanentSpell(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?
    ): ExecutionResult {
        val controllerId = spellComponent.casterId
        val ownerId = cardComponent?.ownerId ?: controllerId

        val cardDef = cardComponent?.cardDefinitionId?.let { cardRegistry.getCard(it) }
        if (cardDef != null && !spellComponent.castFaceDown) {
            pauseForEntersAsCopy(state, spellId, spellComponent, cardComponent, cardDef, controllerId, ownerId)
                ?.let { return it }
        }
        return resolveRemainingEntry(state, spellId, spellComponent, cardComponent)
    }

    /**
     * The as-enters steps after "enters as a copy" has been settled. The copy applies first: CR
     * 614.12 picks the entry replacements from the permanent as it would exist on the battlefield,
     * taking into account replacement effects that already modified its entry. So once the spell
     * has taken on the copied identity, its remaining choices and entry replacements are the copied
     * card's — a Clone copying a "choose a color" permanent makes a color choice of its own; the
     * original's choice is not a copiable value.
     *
     * Called by the clone resumer with the copy already stamped on the spell; reads the spell's
     * current components so the copied definition drives every step.
     */
    fun resolveAfterEntryCopy(state: GameState, spellId: EntityId): ExecutionResult {
        val container = state.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Spell entity not found: $spellId")
        val spellComponent = container.get<SpellOnStackComponent>()
            ?: return ExecutionResult.error(state, "Spell has no SpellOnStackComponent")
        return resolveRemainingEntry(state, spellId, spellComponent, container.get<CardComponent>())
    }

    private fun resolveRemainingEntry(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?
    ): ExecutionResult {
        val controllerId = spellComponent.casterId
        val ownerId = cardComponent?.ownerId ?: controllerId
        val cardDef = cardComponent?.cardDefinitionId?.let { cardRegistry.getCard(it) }
        if (cardDef != null && !spellComponent.castFaceDown) {
            pauseForFirstEntersWithChoice(state, spellId, cardComponent, cardDef, controllerId, ownerId)
                ?.let { return it }
            pauseForRevealCounters(state, spellId, cardComponent, cardDef, controllerId, ownerId)
                ?.let { return it }
            pauseForExileCounters(state, spellId, spellComponent, cardComponent, cardDef, controllerId, ownerId)
                ?.let { return it }
            pauseForDevour(state, spellId, cardComponent, cardDef, controllerId, ownerId)
                ?.let { return it }
        }

        // Check for "pay life or enter tapped" (shock lands) before entering the battlefield
        if (cardDef != null && !spellComponent.castFaceDown) {
            pauseForPayLifeOrEnterTapped(state, spellId, cardComponent, cardDef, controllerId, ownerId)
                ?.let { return it }
        }

        return enterBattlefield(state, spellId, spellComponent, cardComponent, cardDef, controllerId)
    }

    /** Check for EntersAsCopy replacement effect before entering the battlefield (Clone, Mockingbird). */
    private fun pauseForEntersAsCopy(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent,
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        controllerId: EntityId,
        ownerId: EntityId
    ): ExecutionResult? {
        val entersAsCopy = cardDef.script.replacementEffects.filterIsInstance<EntersAsCopy>().firstOrNull()
        if (entersAsCopy != null) {
            // Find candidates to copy. Battlefield copies (Clone) read permanents in play;
            // graveyard copies (Superior Spider-Man) read creature cards across every graveyard.
            val copyFilter = entersAsCopy.copyFilter
            val copyFromGraveyard = entersAsCopy.copyFromZone == Zone.GRAVEYARD
            val candidatePool = if (copyFromGraveyard) {
                state.turnOrder.flatMap { state.getGraveyard(it) }
            } else {
                state.getBattlefield()
            }
            var candidates = candidatePool.filter { entityId ->
                predicateEvaluator.matches(
                    state, state.projectedState, entityId, copyFilter,
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
                val whereDesc = if (copyFromGraveyard) "$filterDesc card in a graveyard" else "$filterDesc"
                // Store the operation that consumes the copy choice.
                val continuation = CloneEntersContinuation(
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    castFaceDown = spellComponent.castFaceDown,
                    additionalSubtypes = entersAsCopy.additionalSubtypes,
                    additionalColors = entersAsCopy.additionalColors,
                    additionalKeywords = entersAsCopy.additionalKeywords,
                    exceptions = entersAsCopy.exceptions,
                    nameOverride = entersAsCopy.nameOverride,
                    powerOverride = entersAsCopy.powerOverride,
                    toughnessOverride = entersAsCopy.toughnessOverride,
                    exileCopiedCard = entersAsCopy.exileCopiedCard,
                    additionalCounters = entersAsCopy.additionalCounters,
                    duration = entersAsCopy.duration
                )
                return state.suspendForDecision(
                    question = { decisionId ->
                        SelectCardsDecision(
                            id = decisionId,
                            playerId = controllerId,
                            prompt = if (entersAsCopy.optional) {
                                "You may choose a $whereDesc to copy"
                            } else {
                                "Choose a $whereDesc to copy"
                            },
                            context = DecisionContext(
                                sourceId = spellId,
                                sourceName = cardComponent.name,
                                phase = DecisionPhase.RESOLUTION
                            ),
                            options = candidates,
                            minSelections = if (entersAsCopy.optional) 0 else 1,
                            maxSelections = 1,
                            // Battlefield copies click permanents in-place; graveyard copies use the
                            // modal card-list overlay (graveyards aren't on the battlefield).
                            useTargetingUI = !copyFromGraveyard
                        )
                    },
                    answer = continuation
                )
            }
            // No matching permanents on battlefield - fall through to enter as itself (0/0)
        }
        return null
    }

    private fun pauseForFirstEntersWithChoice(
        state: GameState,
        spellId: EntityId,
        cardComponent: CardComponent,
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        controllerId: EntityId,
        ownerId: EntityId
    ): ExecutionResult? {
        // Check for EntersWithChoice replacement effects (color first, then creature type, then creature)
        // Process in priority order: COLOR → CREATURE_TYPE → CREATURE_ON_BATTLEFIELD
        // When a card has multiple choices (e.g., Riptide Replicator: color + creature type),
        // the first one pauses; its continuation resumer chains to the next.
        val printedChoices = cardDef.script.replacementEffects.filterIsInstance<EntersWithChoice>()
        // Granted Riot ("Other Spiders you control have riot") is not printed on the entering
        // spell, so synthesize its enters-with choice — one per granting lord (CR 702.136b) —
        // when a battlefield lord grants RIOT to it.
        val grantedRiotCount = com.wingedsheep.engine.mechanics.RiotSynthesis
            .grantedRiotInstanceCount(state, spellId, cardRegistry, predicateEvaluator)
        val syntheticRiotChoice = if (grantedRiotCount > 0) {
            com.wingedsheep.engine.mechanics.RiotSynthesis.RIOT_CHOICE
        } else null
        val entersWithChoices = printedChoices + listOfNotNull(syntheticRiotChoice)
        val firstChoice = entersWithChoices
            .sortedBy { it.choiceType.ordinal }
            .firstOrNull()
        if (firstChoice != null) {
            val isSynthetic = firstChoice === syntheticRiotChoice
            val result = entersWithChoicePrompt.pauseForEntersWithChoice(
                state, spellId, controllerId, ownerId, cardComponent, firstChoice,
                syntheticRiot = isSynthetic,
                syntheticRiotRemaining = if (isSynthetic) grantedRiotCount - 1 else 0
            )
            if (result != null) return result
            // null means choice couldn't be presented (e.g., no creatures on battlefield) — fall through
        }
        return null
    }

    private fun pauseForRevealCounters(
        state: GameState,
        spellId: EntityId,
        cardComponent: CardComponent,
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        controllerId: EntityId,
        ownerId: EntityId
    ): ExecutionResult? {
        // Check for EntersWithRevealCounters replacement effect (Amplify mechanic)
        val revealCountersEffect = cardDef.script.replacementEffects.filterIsInstance<com.wingedsheep.sdk.scripting.EntersWithRevealCounters>().firstOrNull()
        if (revealCountersEffect != null) {
            // Find cards in the reveal source zone that match the effect's filter
            val revealZone = ZoneKey(controllerId, revealCountersEffect.revealSource)
            val predicateContext = PredicateContext(controllerId = controllerId, sourceId = spellId)
            val validCards = state.getZone(revealZone).filter { cardId ->
                predicateEvaluator.matches(state, state.projectedState, cardId, revealCountersEffect.filter, predicateContext)
            }

            if (validCards.isNotEmpty()) {
                val continuation = RevealCountersContinuation(
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    counterType = revealCountersEffect.counterType,
                    countersPerReveal = revealCountersEffect.countersPerReveal
                )
                return state.suspendForDecision(
                    question = { decisionId ->
                        SelectCardsDecision(
                            id = decisionId,
                            playerId = controllerId,
                            prompt = "Reveal cards from your ${revealCountersEffect.revealSource.name.lowercase()} that match ${cardComponent.name} (${revealCountersEffect.countersPerReveal} ${revealCountersEffect.counterType.printed} counter${if (revealCountersEffect.countersPerReveal > 1) "s" else ""} each)",
                            context = DecisionContext(
                                sourceId = spellId,
                                sourceName = cardComponent.name,
                                phase = DecisionPhase.RESOLUTION
                            ),
                            options = validCards,
                            minSelections = 0,
                            maxSelections = validCards.size
                        )
                    },
                    answer = continuation
                )
            }
            // No valid cards — enter normally without counters
        }
        return null
    }

    private fun pauseForExileCounters(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent,
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        controllerId: EntityId,
        ownerId: EntityId
    ): ExecutionResult? {
        val exileCountersEffect = cardDef.script.replacementEffects
            .filterIsInstance<com.wingedsheep.sdk.scripting.EntersWithExileCounters>()
            .firstOrNull()
        if (exileCountersEffect != null) {
            val predicateContext = PredicateContext(controllerId = controllerId, sourceId = spellId)
            val candidates = state.getZone(ZoneKey(controllerId, exileCountersEffect.sourceZone)).filter { cardId ->
                predicateEvaluator.matches(
                    state, state.projectedState, cardId, exileCountersEffect.filter, predicateContext
                )
            }
            val maxCards = amountEvaluator.evaluate(
                state,
                exileCountersEffect.maxCards,
                EffectContext(
                    sourceId = spellId,
                    controllerId = controllerId,
                    xValue = spellComponent.xValue ?: 0
                )
            ).coerceAtLeast(0).coerceAtMost(candidates.size)
            if (candidates.isNotEmpty() && maxCards > 0) {
                val continuation = ExileCountersContinuation(
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    counterType = exileCountersEffect.counterType,
                    countersPerCard = exileCountersEffect.countersPerCard
                )
                return state.suspendForDecision(
                    question = { decisionId ->
                        SelectCardsDecision(
                            id = decisionId,
                            playerId = controllerId,
                            prompt = "Exile up to $maxCards ${exileCountersEffect.filter.description} cards from your ${exileCountersEffect.sourceZone.name.lowercase()} for ${cardComponent.name}",
                            context = DecisionContext(
                                sourceId = spellId,
                                sourceName = cardComponent.name,
                                phase = DecisionPhase.RESOLUTION
                            ),
                            options = candidates,
                            minSelections = 0,
                            maxSelections = maxCards
                        )
                    },
                    answer = continuation
                )
            }
        }
        return null
    }

    private fun pauseForDevour(
        state: GameState,
        spellId: EntityId,
        cardComponent: CardComponent,
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        controllerId: EntityId,
        ownerId: EntityId
    ): ExecutionResult? {
        // Check for EntersWithDevour replacement effect (CR 702.82, Devour variants).
        // Pauses for the controller to pick which permanents to sacrifice; the resumer
        // sacrifices them, places multiplier × count counters on the entering spell
        // entity, then completes the entry.
        val devourEffect = cardDef.script.replacementEffects
            .filterIsInstance<com.wingedsheep.sdk.scripting.EntersWithDevour>().firstOrNull()
        if (devourEffect != null) {
            val candidates = com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
                .devourSacrificeCandidates(state, controllerId, devourEffect, enteringId = spellId, predicateEvaluator = predicateEvaluator)

            if (candidates.isNotEmpty()) {
                val devourLabel = devourEffect.description.substringBefore(" (")
                val continuation = DevourEntersContinuation(
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    multiplier = devourEffect.multiplier,
                    counterType = devourEffect.counterType
                )
                return state.suspendForDecision(
                    question = { decisionId ->
                        SelectCardsDecision(
                            id = decisionId,
                            playerId = controllerId,
                            prompt = "$devourLabel: sacrifice any number of ${devourEffect.sacrificeFilter.description}s for ${cardComponent.name}",
                            context = DecisionContext(
                                sourceId = spellId,
                                sourceName = cardComponent.name,
                                phase = DecisionPhase.RESOLUTION
                            ),
                            options = candidates,
                            minSelections = 0,
                            maxSelections = candidates.size,
                            useTargetingUI = true
                        )
                    },
                    answer = continuation
                )
            }
            // No valid permanents to sacrifice — enter with zero devour counters
        }
        return null
    }

    private fun pauseForPayLifeOrEnterTapped(
        state: GameState,
        spellId: EntityId,
        cardComponent: CardComponent,
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        controllerId: EntityId,
        ownerId: EntityId
    ): ExecutionResult? {
        val entersTapped = cardDef.script.replacementEffects.filterIsInstance<EntersTapped>().firstOrNull()
        if (entersTapped?.payLifeCost != null) {
            val continuation = PayLifeOrEnterTappedSpellContinuation(
                spellId = spellId,
                controllerId = controllerId,
                ownerId = ownerId,
                lifeCost = entersTapped.payLifeCost!!
            )
            return state.suspendForDecision(
                question = { decisionId ->
                    YesNoDecision(
                        id = decisionId,
                        playerId = controllerId,
                        prompt = "Pay ${entersTapped.payLifeCost} life to have ${cardComponent.name} enter untapped?",
                        context = DecisionContext(
                            sourceId = spellId,
                            sourceName = cardComponent.name,
                            phase = DecisionPhase.RESOLUTION
                        )
                    )
                },
                answer = continuation
            )
        }
        return null
    }

    /**
     * Put the permanent onto the battlefield ([PermanentEntry]) once no "as this enters" question
     * is outstanding, then run its generic `OnEnterRun` replacement.
     */
    private fun enterBattlefield(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?,
        controllerId: EntityId
    ): ExecutionResult {
        // Normal permanent entry
        val (afterEntry, enterEvents) = permanentEntry.enterPermanentOnBattlefield(state, spellId, spellComponent, cardComponent, cardDef)
        val isSagaEntry = cardDef != null && !spellComponent.castFaceDown && cardDef.isSaga
        val sagaEvents = if (isSagaEntry) {
            listOf(CountersAddedEvent(spellId, CounterType.LORE, 1, cardDef!!.name))
        } else {
            emptyList()
        }
        val enteredState = if (isSagaEntry) {
            com.wingedsheep.engine.handlers.effects.DamageUtils.markCounterOnControlledPermanent(afterEntry, spellId, CounterType.LORE, entering = true)
        } else {
            afterEntry
        }

        // The generic "as this permanent enters, …" replacement ([OnEnterRun]). The move path
        // (MoveToZoneEffectExecutor) and the land path (PlayLandHandler) already run it; a permanent
        // *cast as a spell* did not, which made the replacement silently inert on every creature and
        // enchantment carrying it — Nameless Race's "as this creature enters, pay any amount of
        // life". Runs after entry, like the move path, so the effect sees a real permanent.
        //
        // The effect may pause for a decision; the paused result carries the entry events with it so
        // the ETB triggers are deferred to the resume path rather than lost, exactly as the move
        // path documents. Skipped for a face-down entry (CR 708.2 — no abilities).
        if (cardDef != null && !spellComponent.castFaceDown) {
            val onEnterResult = com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
                .runOnEnterRunEffect(
                    enteredState, spellId, controllerId, cardRegistry,
                    effects::execute,
                    xValue = spellComponent.xValue,
                )
            if (onEnterResult != null) {
                return onEnterResult.toExecutionResult().copy(
                    events = enterEvents + sagaEvents + onEnterResult.events,
                )
            }
        }

        return ExecutionResult.success(enteredState, enterEvents + sagaEvents)
    }
}
