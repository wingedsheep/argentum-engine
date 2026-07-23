package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ContinuationFrame
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.CloneEntersOnBattlefieldContinuation
import com.wingedsheep.engine.core.EntersWithChoiceOnBattlefieldContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.HandLookedAtEvent
import com.wingedsheep.engine.core.OptionMetadata
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Applies a permanent's own "as-enters" [EntersWithChoice] replacement (CR 614.12 — choose a
 * color / creature type / mode / … as the permanent enters) to an entity that has *already* been
 * placed on the battlefield **directly** — i.e. not cast as a spell that resolves off the stack.
 *
 * Two callers share this seam:
 *  - [com.wingedsheep.engine.handlers.actions.land.PlayLandHandler] — a land played directly, and
 *  - [com.wingedsheep.engine.handlers.effects.token.TokenFromDefinition] — a token minted from a
 *    card definition (e.g. the Momir Basic avatar's random-creature token).
 *
 * The spell-resolution path keeps its own pre-battlefield variant
 * ([com.wingedsheep.engine.mechanics.stack.StackResolver.pauseForEntersWithChoice]) because there
 * the entity is still a `SpellOnStackComponent` on the stack, not yet a permanent.
 *
 * The choice pauses for a player decision. [EntersWithChoiceOnBattlefieldContinuation]'s resumer
 * records the chosen value into the entity's `CastChoicesComponent`, chains to any remaining
 * choice, and then fires the entry's ETB triggers off a synthesized [ZoneChangeEvent] (using the
 * continuation's `fromZone`). **ETB triggers are therefore fired by the resumer, never by the
 * caller** — a caller whose paused result is run through trigger detection (e.g. an effect
 * resolving via `StackResolver`) must NOT also include the entry battlefield [ZoneChangeEvent] in
 * [carryEvents], or the triggers fire twice.
 */
object PermanentEntryReplacements {

    private val predicateEvaluator = PredicateEvaluator()

    /**
     * Models the "look at an opponent's hand" clause of an [EntersWithChoice] with
     * [EntersWithChoice.lookAtOpponentHand] set (Sorcerous Spyglass). Reveals the first opponent's
     * hand to [viewerId] for the rest of the game (via [RevealedToComponent], the same durable
     * reveal used by "look at target player's hand") and emits a [HandLookedAtEvent] so the client
     * shows those cards to the viewer while they make the choice. The reveal is purely
     * informational — it never restricts the name that may be chosen. Returns the (possibly
     * unchanged) state paired with any event to carry.
     */
    fun revealOpponentHandForEntersChoice(
        state: GameState,
        viewerId: EntityId,
    ): Pair<GameState, List<GameEvent>> {
        val opponentId = state.getOpponents(viewerId).firstOrNull() ?: return state to emptyList()
        val handCards = state.getHand(opponentId)
        var newState = state
        for (cardId in handCards) {
            newState = newState.updateEntity(cardId) { container ->
                val existing = container.get<RevealedToComponent>()
                if (existing != null) container.with(existing.withPlayer(viewerId))
                else container.with(RevealedToComponent.to(viewerId))
            }
        }
        return newState to listOf(HandLookedAtEvent(viewerId, opponentId, handCards))
    }

    /**
     * Copy candidates for an [EntersAsCopy] on a permanent already on the battlefield: land/permanent
     * cards in graveyards ([Zone.GRAVEYARD]) or permanents on the battlefield matching
     * [EntersAsCopy.copyFilter], excluding [entityId] itself. Exposed so callers (e.g.
     * `PlayLandHandler`) can guard resource consumption before committing to a pause.
     */
    fun entersAsCopyCandidates(
        state: GameState,
        entityId: EntityId,
        controllerId: EntityId,
        effect: EntersAsCopy,
    ): List<EntityId> {
        val pool = if (effect.copyFromZone == Zone.GRAVEYARD) {
            state.turnOrder.flatMap { state.getGraveyard(it) }
        } else {
            state.getBattlefield()
        }
        return pool.filter { candidateId ->
            candidateId != entityId &&
                predicateEvaluator.matches(
                    state, state.projectedState, candidateId, effect.copyFilter,
                    PredicateContext(controllerId = controllerId)
                )
        }
    }

    /**
     * Build the paused [ExecutionResult] for a permanent's [EntersAsCopy] replacement (CR 707.2) on
     * a permanent already placed on the battlefield **directly** — a land played (Echoing Deeps /
     * Vesuva / Thespian's Stage) or a token/put-onto-battlefield permanent. The spell-resolution
     * path keeps its own pre-battlefield variant
     * ([com.wingedsheep.engine.mechanics.stack.StackResolver.resolvePermanentSpell]) because there
     * the entity is still on the stack.
     *
     * Gathers copy candidates — land/permanent cards in graveyards ([Zone.GRAVEYARD]) or permanents
     * on the battlefield — matching [EntersAsCopy.copyFilter], excluding the entering permanent
     * itself, then pauses for a [SelectCardsDecision] (min 0 when [EntersAsCopy.optional], so "may"
     * declines are expressible). [CloneEntersOnBattlefieldContinuation]'s resumer applies the copy,
     * taps if [EntersAsCopy.tappedIfCopied], and fires the entry's ETB triggers.
     *
     * @return a paused [ExecutionResult], or `null` if there are no candidates to copy — the caller
     *   then completes entry normally (the permanent stays its printed self).
     */
    fun pauseForEntersAsCopy(
        state: GameState,
        entityId: EntityId,
        controllerId: EntityId,
        cardComponent: CardComponent,
        effect: EntersAsCopy,
        fromZone: Zone?,
        carryEvents: List<GameEvent> = emptyList(),
    ): ExecutionResult? {
        val copyFromGraveyard = effect.copyFromZone == Zone.GRAVEYARD
        val candidates = entersAsCopyCandidates(state, entityId, controllerId, effect)
        if (candidates.isEmpty()) return null

        val filterDesc = effect.copyFilter.description
        val whereDesc = if (copyFromGraveyard) "$filterDesc card in a graveyard" else filterDesc
        val decisionId = "clone-enters-bf-${entityId.value}"
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = if (effect.optional) "You may choose a $whereDesc to copy" else "Choose a $whereDesc to copy",
            context = DecisionContext(
                sourceId = entityId,
                sourceName = cardComponent.name,
                phase = DecisionPhase.RESOLUTION
            ),
            options = candidates,
            minSelections = if (effect.optional) 0 else 1,
            maxSelections = 1,
            // Battlefield copies click permanents in-place; graveyard copies use the modal
            // card-list overlay (graveyards aren't on the battlefield).
            useTargetingUI = !copyFromGraveyard
        )
        val continuation = CloneEntersOnBattlefieldContinuation(
            decisionId = decisionId,
            entityId = entityId,
            controllerId = controllerId,
            fromZone = fromZone,
            additionalSubtypes = effect.additionalSubtypes,
            additionalKeywords = effect.additionalKeywords,
            nameOverride = effect.nameOverride,
            powerOverride = effect.powerOverride,
            toughnessOverride = effect.toughnessOverride,
            exileCopiedCard = effect.exileCopiedCard,
            tappedIfCopied = effect.tappedIfCopied,
        )
        val paused = state.pushContinuation(continuation).withPendingDecision(decision)
        return ExecutionResult.paused(paused, decision, carryEvents)
    }

    /**
     * Build the paused [ExecutionResult] for a permanent's first/next [EntersWithChoice].
     *
     * @param entityId the permanent already on the battlefield.
     * @param controllerId its controller.
     * @param cardComponent its card component (for the prompt name).
     * @param choice the choice to present.
     * @param fromZone the zone the permanent came from, used to synthesize the entry
     *   [ZoneChangeEvent] in the resumer; `null` for a freshly-minted token (no prior zone).
     * @param carryEvents events already produced by the caller to forward with the pause (e.g.
     *   counters added). Must NOT include the entry battlefield [ZoneChangeEvent] when the caller's
     *   result is trigger-detected (see class docs).
     * @return a paused [ExecutionResult], or `null` if the choice cannot be presented (e.g.
     *   `CREATURE_ON_BATTLEFIELD` with no other creatures, an empty `MODE`/`OPPONENT` set) — the
     *   caller then completes entry normally.
     */
    fun pauseForEntersWithChoice(
        state: GameState,
        entityId: EntityId,
        controllerId: EntityId,
        cardComponent: CardComponent,
        choice: EntersWithChoice,
        fromZone: Zone?,
        carryEvents: List<GameEvent> = emptyList(),
        cardNameOptions: List<String> = emptyList(),
    ): ExecutionResult? {
        val chooserId = when (choice.chooser) {
            Player.AnOpponent -> state.getOpponents(controllerId).firstOrNull() ?: controllerId
            else -> controllerId
        }
        val name = cardComponent.name

        fun context(decisionId: String) = DecisionContext(
            sourceId = entityId,
            sourceName = name,
            phase = DecisionPhase.RESOLUTION
        )

        fun pause(decision: PendingDecision, continuation: ContinuationFrame): ExecutionResult {
            val paused = state.pushContinuation(continuation).withPendingDecision(decision)
            return ExecutionResult.paused(paused, decision, carryEvents)
        }

        return when (choice.choiceType) {
            ChoiceType.COLOR -> {
                val id = "choose-color-enters-${entityId.value}"
                pause(
                    ChooseColorDecision(id, chooserId, "Choose a color", context(id)),
                    EntersWithChoiceOnBattlefieldContinuation(
                        decisionId = id,
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.COLOR,
                        fromZone = fromZone
                    )
                )
            }

            ChoiceType.CREATURE_TYPE -> {
                val options = choice.allowedCreatureTypes ?: Subtype.ALL_CREATURE_TYPES
                val id = "choose-creature-type-enters-${entityId.value}"
                pause(
                    ChooseOptionDecision(
                        id = id,
                        playerId = chooserId,
                        prompt = "Choose a creature type",
                        context = context(id),
                        options = options,
                        defaultSearch = ""
                    ),
                    EntersWithChoiceOnBattlefieldContinuation(
                        decisionId = id,
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.CREATURE_TYPE,
                        creatureTypes = options,
                        fromZone = fromZone
                    )
                )
            }

            ChoiceType.CREATURE_ON_BATTLEFIELD -> {
                val creatures = state.getBattlefield().filter { eid ->
                    if (eid == entityId) return@filter false
                    val container = state.getEntity(eid) ?: return@filter false
                    val card = container.get<CardComponent>() ?: return@filter false
                    state.projectedState.getController(eid) == controllerId && card.typeLine.isCreature
                }
                if (creatures.isEmpty()) return null
                val id = "choose-creature-enters-${entityId.value}"
                pause(
                    SelectCardsDecision(
                        id = id,
                        playerId = controllerId,
                        prompt = "Choose another creature you control",
                        context = context(id),
                        options = creatures,
                        minSelections = 1,
                        maxSelections = 1,
                        useTargetingUI = true
                    ),
                    EntersWithChoiceOnBattlefieldContinuation(
                        decisionId = id,
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.CREATURE_ON_BATTLEFIELD,
                        fromZone = fromZone
                    )
                )
            }

            ChoiceType.MODE -> {
                if (choice.modeOptions.isEmpty()) return null
                val id = "choose-mode-enters-${entityId.value}"
                pause(
                    ChooseOptionDecision(
                        id = id,
                        playerId = chooserId,
                        prompt = "Choose for $name",
                        context = context(id),
                        options = choice.modeOptions.map { it.label },
                        optionMetadata = choice.modeOptions.map {
                            OptionMetadata(id = it.id, description = it.description, iconKey = it.iconKey)
                        }
                    ),
                    EntersWithChoiceOnBattlefieldContinuation(
                        decisionId = id,
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.MODE,
                        modeOptionIds = choice.modeOptions.map { it.id },
                        fromZone = fromZone
                    )
                )
            }

            ChoiceType.BASIC_LAND_TYPE -> {
                val options = Subtype.ALL_BASIC_LAND_TYPES.toList()
                val id = "choose-land-type-enters-${entityId.value}"
                pause(
                    ChooseOptionDecision(
                        id = id,
                        playerId = chooserId,
                        prompt = "Choose a basic land type",
                        context = context(id),
                        options = options,
                        defaultSearch = ""
                    ),
                    EntersWithChoiceOnBattlefieldContinuation(
                        decisionId = id,
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.BASIC_LAND_TYPE,
                        landTypes = options,
                        fromZone = fromZone
                    )
                )
            }

            ChoiceType.OPPONENT -> {
                val opponentIds = state.turnOrder.filter { it != chooserId }
                if (opponentIds.isEmpty()) return null
                val opponentNames = opponentIds.map { pid ->
                    state.getEntity(pid)?.get<PlayerComponent>()?.name ?: "Player ${pid.value}"
                }
                val id = "choose-opponent-enters-${entityId.value}"
                pause(
                    ChooseOptionDecision(
                        id = id,
                        playerId = chooserId,
                        prompt = "Choose an opponent",
                        context = context(id),
                        options = opponentNames
                    ),
                    EntersWithChoiceOnBattlefieldContinuation(
                        decisionId = id,
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.OPPONENT,
                        opponentIds = opponentIds,
                        fromZone = fromZone
                    )
                )
            }

            ChoiceType.CARD_NAME -> {
                // The option list (land names, or every card name for a CardNamePool.ANY choice) is
                // supplied by the caller, which has the registry in scope. If empty, there is nothing
                // to name — complete entry normally.
                val options = cardNameOptions.sorted()
                if (options.isEmpty()) return null
                // "As this enters, look at an opponent's hand, then …": reveal to the controller
                // before presenting the choice, so they see the hand while naming a card.
                val (baseState, lookEvents) = if (choice.lookAtOpponentHand) {
                    revealOpponentHandForEntersChoice(state, controllerId)
                } else state to emptyList()
                val id = "choose-card-name-enters-${entityId.value}"
                val prompt = if (choice.cardNamePool == CardNamePool.ANY) {
                    "Choose a card name"
                } else "Choose a land card name"
                val decision = ChooseOptionDecision(
                    id = id,
                    playerId = chooserId,
                    prompt = prompt,
                    context = context(id),
                    options = options
                )
                val continuation = EntersWithChoiceOnBattlefieldContinuation(
                    decisionId = id,
                    entityId = entityId,
                    controllerId = controllerId,
                    choiceType = ChoiceType.CARD_NAME,
                    cardNames = options,
                    fromZone = fromZone
                )
                val paused = baseState.pushContinuation(continuation).withPendingDecision(decision)
                ExecutionResult.paused(paused, decision, carryEvents + lookEvents)
            }

            ChoiceType.NUMBER -> {
                // "As this enters, choose a number between [min] and [max]" for a permanent already
                // on the battlefield (token / blink). Stored under [ChoiceSlot.CHOSEN_NUMBER].
                val id = "choose-number-enters-${entityId.value}"
                pause(
                    ChooseNumberDecision(
                        id = id,
                        playerId = chooserId,
                        prompt = "Choose a number between ${choice.minValue} and ${choice.maxValue}",
                        context = context(id),
                        minValue = choice.minValue,
                        maxValue = choice.maxValue
                    ),
                    EntersWithChoiceOnBattlefieldContinuation(
                        decisionId = id,
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.NUMBER,
                        fromZone = fromZone
                    )
                )
            }
        }
    }
}
