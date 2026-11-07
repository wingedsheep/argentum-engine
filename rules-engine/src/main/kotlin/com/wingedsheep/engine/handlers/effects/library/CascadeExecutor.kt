package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.CascadeMayCastContinuation
import com.wingedsheep.engine.core.CascadePutOntoBattlefieldContinuation
import com.wingedsheep.engine.core.CascadeRider
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.standingStaticResults
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.sdk.scripting.CascadeMayPutOntoBattlefield
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.LibraryPlacement
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CascadeEffect
import kotlin.reflect.KClass
import com.wingedsheep.engine.core.Outcome

/**
 * Executor for [CascadeEffect] (CR 702.85).
 *
 * The cascade trigger fires with the triggering spell pointed at by
 * [EffectContext.triggeringEntityId] — either a spell with cascade or, as on
 * Wildsear, Scouring Maw, a spell that was granted cascade. The executor reads
 * that spell's mana value to derive the threshold and walks the controller's
 * library top-down, exiling cards until a nonland card with mana value strictly
 * less than the threshold is exiled (the "cascade card") or the library is
 * exhausted.
 *
 * Before that, each "as you cascade, you may put a land card from among the exiled cards onto
 * the battlefield tapped" rider the controller has standing (Averna, the Chaos Bloom —
 * [CascadeMayPutOntoBattlefield]) offers its card, after the last card is exiled and before the
 * cast offer, whether or not a card to cast was found ([continueCascade]).
 *
 * The "may cast / bottom remainder" flow then follows CR 702.85a:
 *
 *  - If a cascade card was found, pause with a yes/no decision and push a
 *    [com.wingedsheep.engine.core.CascadeMayCastContinuation]; the resumer in
 *    [com.wingedsheep.engine.handlers.continuations.LibraryAndZoneContinuationResumer]
 *    casts the card for free on yes and bottom-randomizes every uncast exiled
 *    card afterwards.
 *  - If no qualifying card was found, every exiled card is bottom-randomized
 *    here and no spell is offered.
 */
class CascadeExecutor(
    private val zones: ZoneTransitionService,
    /** Whether a cast prohibition forbids (player) casting (card) right now (Drannith Magistrate). */
    private val castForbidden: (GameState, EntityId, EntityId) -> Boolean = { _, _, _ -> false },
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<CascadeEffect> {

    override val effectType: KClass<CascadeEffect> = CascadeEffect::class

    override fun execute(
        state: GameState,
        effect: CascadeEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        val triggeringSpellId = context.triggeringEntityId
            ?: return EffectResult.success(state)

        val threshold = state.getEntity(triggeringSpellId)
            ?.get<CardComponent>()?.manaValue
            ?: return EffectResult.success(state)

        var currentState = state
        val allEvents = mutableListOf<EngineGameEvent>()
        val exiledCards = mutableListOf<EntityId>()
        var cascadeCard: EntityId? = null

        val library = currentState.getZone(ZoneKey(controllerId, Zone.LIBRARY))
        for (cardId in library) {
            exiledCards.add(cardId)
            val card = currentState.getEntity(cardId)?.get<CardComponent>()
            if (card != null && !card.typeLine.isLand && card.manaValue < threshold) {
                cascadeCard = cardId
                break
            }
        }

        if (exiledCards.isEmpty()) {
            return EffectResult.success(currentState)
        }

        val sourceName = context.sourceId?.let {
            currentState.getEntity(it)?.get<CardComponent>()?.name
        }
        allEvents.add(
            CardsRevealedEvent(
                revealingPlayerId = controllerId,
                cardIds = exiledCards.toList(),
                cardNames = exiledCards.map { id ->
                    currentState.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown"
                },
                imageUris = exiledCards.map { id ->
                    currentState.getEntity(id)?.get<CardComponent>()?.imageUri
                },
                source = sourceName
            )
        )

        for (cardId in exiledCards) {
            val result = ZoneMovementUtils.moveCardToZone(zones, currentState, cardId, Zone.EXILE)
            if (result.outcome is Outcome.Done) {
                currentState = result.state
                allEvents.addAll(result.events)
            }
        }

        val riders = riders(currentState, zones, controllerId)
        val rest = continueCascade(
            zones = zones,
            castForbidden = castForbidden,
            decisionHandler = decisionHandler,
            state = currentState,
            playerId = controllerId,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences,
            exiledCards = exiledCards.toList(),
            cascadeCardId = cascadeCard,
            riders = riders,
        )
        return if (rest.outcome is Outcome.Paused) EffectResult.propagatePause(rest.state, allEvents + rest.events)
        else EffectResult.success(rest.state, allEvents + rest.events)
    }

    companion object {
        /**
         * The "as you cascade, you may put a … card from among the exiled cards onto the
         * battlefield" riders ([CascadeMayPutOntoBattlefield]) standing for [playerId] — one per
         * instance on a permanent they control.
         */
        fun riders(state: GameState, zones: ZoneTransitionService, playerId: EntityId): List<CascadeRider> =
            standingStaticResults(state, zones.cardRegistry, zones.predicateEvaluator, CascadeMayPutOntoBattlefield::class) { ability, source, controller ->
                if (controller == playerId) CascadeRider(ability, source) else null
            }

        /**
         * The rest of a cascade once the walk has exiled its cards (CR 702.85a): first each
         * [riders] entry offers its card from among the exiled ones — after the last card is
         * exiled, before the cast offer (CR 702.85b) — then the controller is offered [cascadeCardId] for free,
         * or, with no card to cast (or a cast prohibition forbidding it), every card still exiled
         * goes to the bottom in a random order. Pauses for each choice; shared by
         * [CascadeExecutor] and the resumer of [CascadePutOntoBattlefieldContinuation].
         */
        fun continueCascade(
            zones: ZoneTransitionService,
            castForbidden: (GameState, EntityId, EntityId) -> Boolean,
            decisionHandler: DecisionHandler,
            state: GameState,
            playerId: EntityId,
            sourceId: EntityId?,
            objectReferences: ObjectReferenceEnvironment,
            exiledCards: List<EntityId>,
            cascadeCardId: EntityId?,
            riders: List<CascadeRider>,
        ): ExecutionResult {
            val exile = state.getZone(ZoneKey(playerId, Zone.EXILE)).toSet()
            for ((index, rider) in riders.withIndex()) {
                val options = exiledCards.filter { id ->
                    id in exile && zones.predicateEvaluator.matches(
                        state, state.projectedState, id, rider.ability.filter,
                        PredicateContext(controllerId = playerId, sourceId = rider.sourceId)
                    )
                }
                if (options.isEmpty()) continue
                val riderName = state.getEntity(rider.sourceId)?.get<CardComponent>()?.name
                val tapped = if (rider.ability.tapped) " tapped" else ""
                return decisionHandler.createCardSelectionDecision(
                    state = state,
                    playerId = playerId,
                    sourceId = rider.sourceId,
                    sourceName = riderName,
                    prompt = "You may put a ${rider.ability.filter.description} card from among the exiled cards onto the battlefield$tapped",
                    options = options,
                    minSelections = 0,
                    maxSelections = 1,
                    answer = CascadePutOntoBattlefieldContinuation(
                        playerId = playerId,
                        sourceId = sourceId,
                        exiledCards = exiledCards,
                        cascadeCardId = cascadeCardId,
                        rider = rider,
                        remainingRiders = riders.drop(index + 1),
                        objectReferences = objectReferences,
                    ),
                )
            }

            // A rider whose filter took the hit itself put it onto the battlefield: nothing to cast.
            val hit = cascadeCardId?.takeIf { it in exile }
            if (hit == null || castForbidden(state, playerId, hit)) {
                // No card to cast, or a cast prohibition (Drannith Magistrate) forbids it — no
                // offer at all: bottom-randomize every card still exiled this way and finish.
                var finalState = state
                val bottomEvents = bottomRandomize(zones, state, playerId, exiledCards.filter { it in exile }) {
                    finalState = it
                }
                return ExecutionResult.success(finalState, bottomEvents)
            }

            val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            val cascadeName = state.getEntity(hit)?.get<CardComponent>()?.name ?: "the exiled card"
            return decisionHandler.createYesNoDecision(
                state = state,
                playerId = playerId,
                sourceId = sourceId,
                sourceName = sourceName,
                prompt = "Cast $cascadeName without paying its mana cost?",
                yesText = "Cast for free",
                noText = "Decline",
                phase = DecisionPhase.RESOLUTION,
                answer = CascadeMayCastContinuation(
                    playerId = playerId,
                    sourceId = sourceId,
                    objectReferences = objectReferences,
                    exiledCards = exiledCards,
                    cascadeCardId = hit,
                ),
            )
        }

        /**
         * Move [cards] (each currently in exile) to the bottom of [playerId]'s library
         * in a random order. Returns the emitted zone-change events; the caller wires
         * the updated state through [updateState] so we can keep [currentState] mutable
         * outside the helper.
         */
        fun bottomRandomize(
            zones: ZoneTransitionService,
            state: GameState,
            playerId: EntityId,
            cards: List<EntityId>,
            updateState: (GameState) -> Unit
        ): List<EngineGameEvent> {
            val events = mutableListOf<EngineGameEvent>()
            var current = state
            val (shuffledCards, advanced) = current.nextRandom { shuffle(cards) }
            current = advanced
            for (cardId in shuffledCards) {
                val result = zones.moveToZone(
                    state = current,
                    entityId = cardId,
                    destinationZone = Zone.LIBRARY,
                    options = ZoneEntryOptions(
                        controllerId = playerId,
                        libraryPlacement = LibraryPlacement.Bottom
                    )
                )
                current = result.state
                events.addAll(result.events)
            }
            updateState(current)
            return events
        }
    }
}
