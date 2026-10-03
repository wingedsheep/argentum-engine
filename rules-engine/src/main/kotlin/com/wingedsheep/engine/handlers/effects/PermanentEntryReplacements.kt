package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.mechanics.stack.colorChoicePrompt
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.AnswerContinuation
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.CloneEntersOnBattlefieldContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.EntersWithChoiceOnBattlefieldContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.HandLookedAtEvent
import com.wingedsheep.engine.core.OptionMetadata
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntersWithDevour
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Applies a permanent's own "as-enters" replacements — [EntersWithChoice] (CR 614.12 — choose a
 * color / creature type / mode / … as the permanent enters), [EntersAsCopy], and the generic
 * [OnEnterRun] — to an entity that has *already* been placed on the battlefield
 * **directly**, i.e. not cast as a spell that resolves off the stack.
 *
 * **Each caller uses a different subset — this is a toolbox, not one entry point.** Which of the
 * two helper families is wired where today:
 *
 *  - [com.wingedsheep.engine.handlers.actions.land.PlayLandHandler] — a land played directly.
 *    Both: [pauseForEntersWithChoice] / [entersAsCopyCandidates], and [OnEnterRun] inline
 *    (sharing this object's [onEnterRunEffectFor] lookup).
 *  - [com.wingedsheep.engine.handlers.effects.token.TokenFromDefinition] — a token minted from a
 *    card definition (e.g. the Momir Basic avatar's random-creature token).
 *    [pauseForEntersWithChoice] and [devourSacrificeCandidates].
 *  - [com.wingedsheep.engine.handlers.effects.zones.MoveToZoneEffectExecutor] — a card put onto
 *    the battlefield by an effect (reanimation, a blink or earthbend return from exile).
 *    [runOnEnterRunEffect], and [EntersWithChoice] through [EffectEntryChoices], which asks
 *    *before* the move (so it shares only [entersChoicePrompt] / [decodeEntersChoice]).
 *    [com.wingedsheep.engine.handlers.effects.library.MoveCollectionExecutor] asks the same way.
 *  - [com.wingedsheep.engine.mechanics.stack.StackResolver] — a permanent *cast as a spell*, run
 *    just after `enterPermanentOnBattlefield`. [runOnEnterRunEffect] only. Added for Nameless
 *    Race; until then [OnEnterRun] was silently inert on every cast permanent, which went
 *    unnoticed because its only two users were lands (played, not cast).
 *  - [com.wingedsheep.engine.handlers.continuations.ModalAndCloneContinuationResumer]'s
 *    `resumeEntersWithChoiceSpell` — the same cast-as-a-spell entry, finished on the *other* side
 *    of an [EntersWithChoice] pause. [runOnEnterRunEffect] only, and it has to be repeated there
 *    because that resumer calls `enterPermanentOnBattlefield` itself: a card carrying both
 *    replacements (Grifter's Blade) otherwise made its choice and then lost the effect that reads
 *    it. `enterPermanentOnBattlefield` can't own the call — it returns a `(GameState, events)`
 *    pair, and this replacement may pause.
 *
 * Those omissions are real gaps, not deliberate exclusions — e.g. `MoveCollectionExecutor` still
 * doesn't run [OnEnterRun].
 *
 * The spell-resolution path keeps its own pre-battlefield variant
 * ([com.wingedsheep.engine.mechanics.stack.StackResolver.pauseForEntersWithChoice]) because there
 * the entity is still a `SpellOnStackComponent` on the stack, not yet a permanent.
 *
 * The choice pauses for a player decision. [EntersWithChoiceOnBattlefieldContinuation]'s resumer
 * records the chosen value into the entity's `CastChoicesComponent`, chains to any remaining
 * choice, and then emits the entry's [ZoneChangeEvent] (using the continuation's `fromZone`).
 * **The entry event is therefore emitted by the resumer, never by the caller.** The settle
 * boundary detects triggers over every event a paused result carries, so a caller must NOT
 * include the entry battlefield [ZoneChangeEvent] in [carryEvents], or the ETB triggers fire
 * twice.
 */
object PermanentEntryReplacements {

    /**
     * Models the "look at an opponent's hand" clause of an [EntersWithChoice] with
     * [EntersWithChoice.lookAtOpponentHand] set (Sorcerous Spyglass). Reveals the first opponent's
     * hand to [viewerId] for the rest of the game (via [RevealedToComponent], the same durable
     * reveal used by "look at target player's hand") and emits a [HandLookedAtEvent] so the client
     * shows those cards to the viewer while they make the choice. The reveal is purely
     * informational — it never restricts the name that may be chosen. Returns the (possibly
     * unchanged) state paired with any event to carry.
     *
     * Two-player scope: the real card lets the controller choose *which* opponent's hand to look at,
     * but the engine's supported games are two-player, so the sole opponent (`firstOrNull`) is
     * unambiguous. A 3+ player game would under-approximate here (same simplification other
     * "an opponent" effects document, e.g. Kitesail Larcenist's "for each player" winnow).
     */
    fun revealOpponentHandForEntersChoice(
        state: GameState,
        viewerId: EntityId,
    ): Pair<GameState, List<GameEvent>> {
        val opponentId = state.getOpponents(viewerId).firstOrNull() ?: return state to emptyList()
        val handCards = state.getHand(opponentId)
        val observers = setOf(state.actorFor(viewerId)) - viewerId
        val newState = com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils.markRevealed(
            state, handCards, observers + viewerId)
        return newState to listOf(HandLookedAtEvent(viewerId, opponentId, handCards, observers))
    }

    /**
     * The single [OnEnterRun] a card definition contributes, or `null` if it has none.
     *
     * **First one wins.** Both entry paths consult only the first, so a card that needs two
     * "as ~ enters" clauses must fold them into one composite inside a single replacement — see
     * `MultiversalPassage`, whose choose-a-type and pay-2-life clauses share one wrapper for
     * exactly this reason. That rule is single-sourced here so
     * [com.wingedsheep.engine.handlers.actions.land.PlayLandHandler] and [runOnEnterRunEffect]
     * cannot drift apart on it.
     */
    /**
     * The permanents [controllerId] may sacrifice for a Devour as-enters replacement (CR 702.82) —
     * every battlefield permanent they *currently* control matching [devour]'s sacrifice filter,
     * minus the entering object itself.
     *
     * Control is read off the projected state so an opponent's Act of Treason on one of your lands
     * removes it from the pool (CR 701.21a: you can only sacrifice permanents you control). The
     * filter match is likewise projected, per the project's battlefield-filter rule.
     *
     * Shared by the two entry paths that can meet a devour permanent: a spell resolving off the
     * stack ([com.wingedsheep.engine.mechanics.stack.StackResolver], where [enteringId] is the
     * spell entity) and a token minted from a bare definition
     * ([com.wingedsheep.engine.handlers.effects.token.TokenFromDefinition], where the token does
     * not exist yet and [enteringId] is null).
     */
    fun devourSacrificeCandidates(
        state: GameState,
        controllerId: EntityId,
        devour: EntersWithDevour,
        enteringId: EntityId?,
        predicateEvaluator: PredicateEvaluator
    ): List<EntityId> {
        val predicateContext = PredicateContext(controllerId = controllerId, sourceId = enteringId)
        return state.getBattlefield().filter { entityId ->
            if (entityId == enteringId) return@filter false
            if (state.projectedState.getController(entityId) != controllerId) return@filter false
            predicateEvaluator.matches(
                state, state.projectedState, entityId, devour.sacrificeFilter, predicateContext
            )
        }
    }

    fun onEnterRunEffectFor(cardDef: CardDefinition?): OnEnterRun? =
        cardDef?.script?.replacementEffects
            ?.filterIsInstance<OnEnterRun>()
            ?.firstOrNull()

    /**
     * Run a permanent's own [OnEnterRun] — the generic "as ~ enters, run [effect]"
     * self-replacement — on an entity that has *already* been placed on the battlefield.
     *
     * [com.wingedsheep.engine.handlers.actions.land.PlayLandHandler] runs this inline for a land
     * being played; this is the counterpart for every *other* direct battlefield entry (a return
     * from exile or the graveyard, a reanimation). Without it, a permanent whose entry choice
     * lives in this replacement — Multiversal Passage's "as this land enters, choose a basic land
     * type" — comes back with the choice never made and no way to make it later.
     *
     * Not a library search that puts a card onto the battlefield: `SearchDestination.BATTLEFIELD`
     * routes through `MoveCollectionExecutor` even for a single card, and that executor does not
     * run this replacement yet.
     *
     * The effect runs with a fresh [EffectContext] rooted at the entering permanent, so
     * `EffectTarget.Self` resolves to it rather than to whatever moved it. It may pause for player
     * input like any other effect; the caller forwards the pause (with the entry events it has
     * already collected) and the normal continuation machinery finishes the rest.
     *
     * @param resolutionDepth the calling effect's depth, carried into the fresh context so the
     *   registry's runaway-recursion backstop still counts a self-perpetuating entry loop.
     * @return the [EffectResult] of running the replacement, or `null` if the card has no
     *   [OnEnterRun] — the caller then completes entry normally.
     */
    fun runOnEnterRunEffect(
        state: GameState,
        entityId: EntityId,
        controllerId: EntityId,
        cardRegistry: CardRegistry,
        effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
        resolutionDepth: Int = 0,
        /**
         * The X chosen for the spell that is entering, when there was one. An as-enters clause on
         * an {X} permanent reads it (Frankenstein's Monster: "exile X creature cards"), and without
         * it `DynamicAmount.XValue` silently evaluates to 0 — the clause then does nothing at all
         * rather than failing. Null on the paths where nothing was cast (a land played, a permanent
         * put onto the battlefield by an effect).
         */
        xValue: Int? = null,
    ): EffectResult? {
        val cardDefinitionId = state.getEntity(entityId)?.get<CardComponent>()?.cardDefinitionId ?: return null
        val onEnter = onEnterRunEffectFor(cardRegistry.getCard(cardDefinitionId)) ?: return null
        val enteredRef = state.objectRef(entityId)
        return effectExecutor(
            state,
            onEnter.effect,
            EffectContext(
                sourceId = entityId,
                controllerId = controllerId,
                objectReferences = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(
                    captured = true, origin = enteredRef, source = enteredRef,
                    resolutionKey = "entry:$entityId:${enteredRef?.generation}"
                ),
                resolutionDepth = resolutionDepth,
                xValue = xValue,
            ),
        )
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
        predicateEvaluator: PredicateEvaluator
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
        entryOldObject: com.wingedsheep.engine.state.ObjectRef? = null,
        entryNewObject: com.wingedsheep.engine.state.ObjectRef? = state.objectRef(entityId),
        predicateEvaluator: PredicateEvaluator
    ): ExecutionResult? {
        val copyFromGraveyard = effect.copyFromZone == Zone.GRAVEYARD
        val candidates = entersAsCopyCandidates(state, entityId, controllerId, effect, predicateEvaluator = predicateEvaluator)
        if (candidates.isEmpty()) return null

        val filterDesc = effect.copyFilter.description
        val whereDesc = if (copyFromGraveyard) "$filterDesc card in a graveyard" else filterDesc
        val question = { decisionId: String -> SelectCardsDecision(
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
        ) }
        val continuation = CloneEntersOnBattlefieldContinuation(
            entityId = entityId,
            controllerId = controllerId,
            fromZone = fromZone,
            additionalSubtypes = effect.additionalSubtypes,
            additionalColors = effect.additionalColors,
            additionalKeywords = effect.additionalKeywords,
            exceptions = effect.exceptions,
            nameOverride = effect.nameOverride,
            powerOverride = effect.powerOverride,
            toughnessOverride = effect.toughnessOverride,
            exileCopiedCard = effect.exileCopiedCard,
            tappedIfCopied = effect.tappedIfCopied,
            additionalCounters = effect.additionalCounters,
            duration = effect.duration,
            entryOldObject = entryOldObject,
            entryNewObject = entryNewObject,
        )
        return state.suspendForDecision(question, continuation, carryEvents)
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
     *   counters added). Must NOT include the entry battlefield [ZoneChangeEvent] (see class docs).
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
        syntheticRiot: Boolean = false,
        syntheticRiotRemaining: Int = 0,
        entryOldObject: com.wingedsheep.engine.state.ObjectRef? = null,
        entryNewObject: com.wingedsheep.engine.state.ObjectRef? = state.objectRef(entityId),
        copyOfOriginalName: String? = null,
    ): ExecutionResult? {
        val prompt = entersChoicePrompt(
            state, entityId, controllerId, cardComponent, choice, fromZone,
            cardNameOptions, syntheticRiot, syntheticRiotRemaining
        ) ?: return null
        // The entry's object identities are captured on the answer, not read when it resumes:
        // by then the permanent has finished entering and the old object is gone.
        return prompt.state.suspendForDecision(
            prompt.question,
            prompt.answer.copy(
                entryOldObject = entryOldObject, entryNewObject = entryNewObject,
                copyOfOriginalName = copyOfOriginalName,
            ),
            carryEvents + prompt.events,
        )
    }

    /**
     * Read the player's answer to an [entersChoicePrompt] question into the [ChoiceSlot] it fills
     * and the value to record there, or `null` when the response doesn't fit the question.
     */
    fun decodeEntersChoice(
        question: EntersWithChoiceOnBattlefieldContinuation,
        response: DecisionResponse,
    ): Pair<ChoiceSlot, ChoiceValue>? {
        fun <T> option(options: List<T>): T? = (response as? OptionChosenResponse)?.let { options.getOrNull(it.optionIndex) }
        return when (question.choiceType) {
            ChoiceType.COLOR -> (response as? ColorChosenResponse)
                ?.let { ChoiceSlot.COLOR to ChoiceValue.ColorChoice(it.color) }
            ChoiceType.CREATURE_TYPE -> option(question.creatureTypes)
                ?.let { ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice(it) }
            ChoiceType.CREATURE_ON_BATTLEFIELD -> (response as? CardsSelectedResponse)?.selectedCards?.firstOrNull()
                ?.let { ChoiceSlot.CREATURE to ChoiceValue.EntityChoice(it) }
            ChoiceType.MODE -> option(question.modeOptionIds)
                ?.let { ChoiceSlot.MODE to ChoiceValue.TextChoice(it) }
            ChoiceType.BASIC_LAND_TYPE -> option(question.landTypes)
                ?.let { ChoiceSlot.LAND_TYPE to ChoiceValue.TextChoice(it) }
            ChoiceType.OPPONENT -> option(question.opponentIds)
                ?.let { ChoiceSlot.OPPONENT to ChoiceValue.EntityChoice(it) }
            ChoiceType.CARD_NAME -> option(question.cardNames)
                ?.let { ChoiceSlot.CARD_NAME to ChoiceValue.TextChoice(it) }
            ChoiceType.NUMBER -> (response as? NumberChosenResponse)
                ?.let { ChoiceSlot.CHOSEN_NUMBER to ChoiceValue.NumberChoice(it.number) }
        }
    }

    /**
     * An [EntersWithChoice] question, not yet asked: the decision to present, and the answer frame
     * that knows how to read the response ([decodeEntersChoice]). [state] and [events] carry the
     * reveal a [EntersWithChoice.lookAtOpponentHand] choice makes before it is asked.
     */
    class EntersChoicePrompt(
        val question: (String) -> PendingDecision,
        val answer: EntersWithChoiceOnBattlefieldContinuation,
        val state: GameState,
        val events: List<GameEvent> = emptyList(),
    )

    /**
     * Build the question for one [EntersWithChoice] of [entityId] — shared by the post-entry pause
     * above and the pre-entry preparation of an effect-driven entry
     * ([com.wingedsheep.engine.handlers.effects.EffectEntryChoices]).
     *
     * @return `null` if the choice cannot be presented (see [pauseForEntersWithChoice]).
     */
    fun entersChoicePrompt(
        state: GameState,
        entityId: EntityId,
        controllerId: EntityId,
        cardComponent: CardComponent,
        choice: EntersWithChoice,
        fromZone: Zone?,
        cardNameOptions: List<String> = emptyList(),
        syntheticRiot: Boolean = false,
        syntheticRiotRemaining: Int = 0,
    ): EntersChoicePrompt? {
        val chooserId = when (choice.chooser) {
            Player.AnOpponent -> state.getOpponents(controllerId).firstOrNull() ?: controllerId
            else -> controllerId
        }
        val name = cardComponent.name

        fun context() = DecisionContext(
            sourceId = entityId,
            sourceName = name,
            phase = DecisionPhase.RESOLUTION
        )

        fun pause(
            question: (String) -> PendingDecision,
            continuation: EntersWithChoiceOnBattlefieldContinuation,
        ) = EntersChoicePrompt(question, continuation, state)

        return when (choice.choiceType) {
            ChoiceType.COLOR -> {
                pause(
                    { id -> ChooseColorDecision(
                        id, chooserId, colorChoicePrompt(choice), context(),
                        availableColors = Color.entries.toSet() - choice.excludedColors
                    ) },
                    EntersWithChoiceOnBattlefieldContinuation(
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.COLOR,
                        fromZone = fromZone
                    )
                )
            }

            ChoiceType.CREATURE_TYPE -> {
                val options = choice.allowedCreatureTypes ?: Subtype.ALL_CREATURE_TYPES
                pause(
                    { id -> ChooseOptionDecision(
                        id = id,
                        playerId = chooserId,
                        prompt = "Choose a creature type",
                        context = context(),
                        options = options,
                        defaultSearch = ""
                    ) },
                    EntersWithChoiceOnBattlefieldContinuation(
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
                    state.projectedState.getController(eid) == controllerId &&
                        state.projectedState.isCreature(eid)
                }
                if (creatures.isEmpty()) return null
                pause(
                    { id -> SelectCardsDecision(
                        id = id,
                        playerId = controllerId,
                        // Same wording rule as the spell path: "another" only when the entering
                        // permanent is itself a creature.
                        prompt = if (cardComponent.isCreature) {
                            "Choose another creature you control"
                        } else {
                            "Choose a creature you control"
                        },
                        context = context(),
                        options = creatures,
                        minSelections = 1,
                        maxSelections = 1,
                        useTargetingUI = true
                    ) },
                    EntersWithChoiceOnBattlefieldContinuation(
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.CREATURE_ON_BATTLEFIELD,
                        fromZone = fromZone
                    )
                )
            }

            ChoiceType.MODE -> {
                if (choice.modeOptions.isEmpty()) return null
                pause(
                    { id -> ChooseOptionDecision(
                        id = id,
                        playerId = chooserId,
                        prompt = "Choose for $name",
                        context = context(),
                        options = choice.modeOptions.map { it.label },
                        optionMetadata = choice.modeOptions.map {
                            OptionMetadata(id = it.id, description = it.description, iconKey = it.iconKey)
                        }
                    ) },
                    EntersWithChoiceOnBattlefieldContinuation(
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.MODE,
                        modeOptionIds = choice.modeOptions.map { it.id },
                        fromZone = fromZone,
                        syntheticRiot = syntheticRiot,
                        syntheticRiotRemaining = syntheticRiotRemaining
                    )
                )
            }

            ChoiceType.BASIC_LAND_TYPE -> {
                val options = Subtype.ALL_BASIC_LAND_TYPES.toList()
                pause(
                    { id -> ChooseOptionDecision(
                        id = id,
                        playerId = chooserId,
                        prompt = "Choose a basic land type",
                        context = context(),
                        options = options,
                        defaultSearch = ""
                    ) },
                    EntersWithChoiceOnBattlefieldContinuation(
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
                pause(
                    { id -> ChooseOptionDecision(
                        id = id,
                        playerId = chooserId,
                        prompt = "Choose an opponent",
                        context = context(),
                        options = opponentNames
                    ) },
                    EntersWithChoiceOnBattlefieldContinuation(
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
                val prompt = choice.cardNamePool.prompt
                val question = { decisionId: String -> ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = prompt,
                    context = context(),
                    options = options
                ) }
                val continuation = EntersWithChoiceOnBattlefieldContinuation(
                    entityId = entityId,
                    controllerId = controllerId,
                    choiceType = ChoiceType.CARD_NAME,
                    cardNames = options,
                    fromZone = fromZone
                )
                EntersChoicePrompt(question, continuation, baseState, lookEvents)
            }

            ChoiceType.NUMBER -> {
                // "As this enters, choose a number between [min] and [max]" for a permanent already
                // on the battlefield (token / blink). Stored under [ChoiceSlot.CHOSEN_NUMBER].
                pause(
                    { id -> ChooseNumberDecision(
                        id = id,
                        playerId = chooserId,
                        prompt = "Choose a number between ${choice.minValue} and ${choice.maxValue}",
                        context = context(),
                        minValue = choice.minValue,
                        maxValue = choice.maxValue
                    ) },
                    EntersWithChoiceOnBattlefieldContinuation(
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
