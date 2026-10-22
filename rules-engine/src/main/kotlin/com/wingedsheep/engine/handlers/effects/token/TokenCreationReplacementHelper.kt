package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TokenCreationReplacementContinuation
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EntersWithReplacements
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.replacement.ActiveReplacement
import com.wingedsheep.engine.replacement.ActiveReplacements
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.TokenReplacementOfferedThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.copiableCardComponent
import com.wingedsheep.engine.state.components.identity.copiableDoubleFacedComponent
import com.wingedsheep.engine.handlers.effects.copy.CopyExceptionApplier
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalTokenScale
import com.wingedsheep.sdk.scripting.CreateAdditionalToken
import com.wingedsheep.sdk.scripting.MultiplyTokenCreation
import com.wingedsheep.sdk.scripting.EventPattern as SdkGameEvent
import com.wingedsheep.sdk.scripting.ModifyTokenCount
import com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithAttachedCopy
import com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithToken
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player

/** What [TokenCreationReplacementHelper.findTokenSubstitution] found for a token-creation event. */
sealed interface TokenSubstitutionLookup

/**
 * A [ReplaceTokenCreationWithToken] that applies to a token-creation event: the replacement in
 * force ([applied], kept so a chained re-check can skip it — CR 614.5) and the [token] it creates
 * instead.
 */
data class TokenSubstitution(val applied: ActiveReplacement, val token: CreateTokenEffect) : TokenSubstitutionLookup

/**
 * A [ReplaceTokenCreationWithToken] that applies but [asks its controller][ReplaceTokenCreationWithToken.asksForChoice]
 * which substitute to use (or, when optional, whether to use one) and hasn't been answered for
 * this event yet. The answer is recorded in the effect context's chosen values under [key]; the
 * creating effect then runs again from the top and reads it.
 */
data class TokenSubstitutionChoice(
    val applied: ActiveReplacement,
    val effect: ReplaceTokenCreationWithToken,
    val key: String
) : TokenSubstitutionLookup

/**
 * The parts of a token-creating instruction that aren't the tokens' characteristics — "tapped",
 * "attacking", "exile/sacrifice them at the beginning of the next end step". A token substitution
 * replaces the characteristics only; these still apply to the substitutes (the rulings on
 * Draconic Visitor, Fisher's Talent and Jinnie Fay, Jetmir's Second).
 */
data class TokenRiders(
    val tapped: Boolean = false,
    val attacking: Boolean = false,
    val attackDefender: com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender =
        com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender.JOIN_SOURCE,
    /** "Attacking that player" — see [CreateTokenEffect.attackedPlayer]. */
    val attackedPlayer: com.wingedsheep.sdk.scripting.targets.EffectTarget? = null,
    val exileAtStep: com.wingedsheep.sdk.core.Step? = null,
    val sacrificeAtStep: com.wingedsheep.sdk.core.Step? = null,
    /** "Tokens created with this creature" provenance (Tetravus). */
    val stampCreator: Boolean = false,
    /** "Create a token with a +1/+1 counter on it." */
    val initialCounters: Map<com.wingedsheep.sdk.core.CounterType, Int> = emptyMap()
) {
    companion object {
        fun of(effect: CreateTokenEffect) = TokenRiders(
            effect.tapped, effect.attacking, effect.attackDefender, effect.attackedPlayer, effect.exileAtStep,
            effect.sacrificeAtStep, effect.stampCreator, effect.initialCounters
        )
    }
}

/**
 * Checks for token creation replacement effects (e.g., Mirrormind Crown)
 * before tokens are created. If a replacement applies, returns a paused
 * EffectResult with a yes/no decision; otherwise returns null.
 */

object TokenCreationReplacementHelper {

    /**
     * "Whose tokens" test: whether the player the tokens are being created under is the one the
     * event's [Player] names, relative to the replacement's own controller — `You` matches when
     * that's the replacement's controller, `EachOpponent` when it's an opponent of theirs, `Any`
     * always. No token entity exists yet, so this is read off the player alone. Shared by the count
     * and additional-token read paths so a printed ability and a durational grant are dispatched
     * identically.
     */
    private fun controllerMatches(
        player: Player,
        state: GameState,
        sourceControllerId: EntityId,
        tokenControllerId: EntityId,
        predicateEvaluator: PredicateEvaluator
    ): Boolean = predicateEvaluator.matchesPlayer(
        state, state.projectedState, player, tokenControllerId, PredicateContext(controllerId = sourceControllerId)
    )

    /**
     * Apply token-count replacement effects whose [SdkGameEvent.TokenCreationEvent] filter
     * matches the player receiving the tokens:
     * - [MultiplyTokenCreation] (Anointed Procession / Exalted Sunborn — factor 2; Ojer Taq —
     *   factor 3) multiplies the count by its factor per source; stacks multiplicatively when
     *   several apply.
     * - [ModifyTokenCount] shifts the count by a fixed amount per source (clamped at zero).
     *
     * Both printed abilities on battlefield permanents *and* durational grants in
     * [com.wingedsheep.engine.state.GameState.grantedReplacementEffects] count — the two are
     * enumerated together by [ActiveReplacements], so Kaya, Geist Hunter's until-end-of-turn
     * doubler stacks with a Doubling Season exactly as a second printed doubler would, and
     * keeps working after Kaya herself has left the battlefield.
     *
     * Dispatch reads `appliesTo.controller`, mirroring [ReplacementEffectUtils.applyCounterPlacementModifiers]:
     * `You` matches when the replacement's controller is the player receiving the tokens,
     * `Opponent` matches when it isn't, `Any` always matches.
     *
     * CR 616.1 hands the order to the affected player when both kinds apply. We default to
     * modifier-then-doublers, which maximizes the count for positive modifiers (e.g. base 3
     * with +1 mod and ×2 doubler: 8 vs 7) and matches what a player optimizing for more
     * tokens would pick. A proper "choose the order" prompt can land when a card actually
     * combines the two kinds.
     */
    fun applyCountReplacements(
        state: GameState,
        tokenControllerId: EntityId,
        baseCount: Int,
        predicateEvaluator: PredicateEvaluator
    ): Int {
        return applyBatchCountReplacements(state, tokenControllerId, listOf(baseCount), predicateEvaluator).single()
    }

    /** Count modifiers affect one joint event; declaration order selects which templates change. */
    fun applyBatchCountReplacements(
        state: GameState,
        tokenControllerId: EntityId,
        baseCounts: List<Int>,
        predicateEvaluator: PredicateEvaluator,
    ): List<Int> {
        if (baseCounts.none { it > 0 }) return baseCounts

        val factors = mutableListOf<Int>()
        var modifier = 0
        for (active in ActiveReplacements.all(state)) {
            val effect = active.effect
            val event = when (effect) {
                is MultiplyTokenCreation -> effect.appliesTo
                is ModifyTokenCount -> effect.appliesTo
                else -> continue
            }
            if (event !is SdkGameEvent.TokenCreationEvent) continue
            // tokenFilter would need a synthetic token-template snapshot to match
            // against; no card uses it yet, so we conservatively skip filtered events
            // rather than treating them as match-all.
            if (event.tokenFilter != null) continue
            if (!controllerMatches(event.controller, state, active.controllerId, tokenControllerId, predicateEvaluator = predicateEvaluator)) continue
            when (effect) {
                is MultiplyTokenCreation -> factors += effect.factor
                is ModifyTokenCount -> modifier += effect.modifier
            }
        }

        // Saturating arithmetic: a stack of doublers (Doubling Season + Anointed Procession + …)
        // multiplies geometrically, which would overflow `Int` to a negative count. Clamp instead
        // (GameLimits); CreateTokenExecutor applies the structural MAX_TOKENS_PER_EFFECT cap before
        // actually allocating entities.
        var remainingModifier = modifier
        return baseCounts.map { original ->
            val adjusted = when {
                original <= 0 -> original
                remainingModifier >= 0 -> com.wingedsheep.engine.core.GameLimits.addClamped(original, remainingModifier)
                    .also { remainingModifier = 0 }
                else -> {
                    val remove = minOf(original.toLong(), -remainingModifier.toLong()).toInt()
                    remainingModifier += remove
                    original - remove
                }
            }
            var count = adjusted
            for (factor in factors) count = com.wingedsheep.engine.core.GameLimits.mulClamped(count, factor)
            count
        }
    }

    /**
     * Apply [CreateAdditionalToken] replacement effects after a batch of tokens has been
     * created. Models Worldwalker Helm ("instead create those tokens plus an additional Map
     * token"), Chatterfang ("those tokens plus that many 1/1 green Squirrel creature tokens") and
     * Academy Manufactor ("instead create one of each").
     *
     * Called by the token-creation executors *after* they place their batch, passing the
     * IDs of the tokens just created and the player who created them. Every active
     * [CreateAdditionalToken] (printed on a battlefield permanent or granted — see
     * [ActiveReplacements]) whose `appliesTo` controller filter matches and whose `tokenFilter`
     * (if any) matches at least one token of the batch adds its tokens: once per event, or once
     * per matching token under [AdditionalTokenScale.PER_MATCHING_TOKEN].
     *
     * Each replacement modifies the event once (CR 614.5), and the next one applies to the
     * event as modified — so the batch grows source by source. The entries one source carries are
     * a single ability: they all judge the batch as it stood before that source applied, so
     * Academy Manufactor's six entries never feed each other, while a second Manufactor sees the
     * first one's Clues, Foods and Treasures (the ruling: two Manufactors make three of each). No
     * source re-judges its own additions, so nothing loops.
     *
     * CR 616.1 lets the affected player order the replacements; we apply them in the order that
     * yields the most tokens — fixed-count riders before "that many" riders; unfiltered fixed-count
     * riders before filtered ones (Peregrin Took's Food can earn Worldwalker Helm's Map), and filtered
     * "that many" riders before unfiltered ones — so Chatterfang counts Quina's Frog and Manufactor's
     * whole set.
     *
     * @return the updated state plus the created additional-token events (empty if none applied).
     */
    fun applyAdditionalTokenReplacements(
        state: GameState,
        tokenControllerId: EntityId,
        createdTokenIds: List<EntityId>,
        originalTapped: Boolean,
        cardRegistry: CardRegistry?,
        staticAbilityHandler: StaticAbilityHandler?,
        predicateEvaluator: PredicateEvaluator,
        eligibleSources: Set<EntityId>? = null,
    ): Pair<GameState, List<com.wingedsheep.engine.core.GameEvent>> {
        if (createdTokenIds.isEmpty() || cardRegistry == null) return state to emptyList()
        val conditionEvaluator = predicateEvaluator.conditions

        val riders = ActiveReplacements.all(state).filter { it.effect is CreateAdditionalToken && (eligibleSources == null || it.sourceId in eligibleSources) }
        if (riders.isEmpty()) return state to emptyList()
        val bySource = riders.groupBy { it.sourceId }.values.sortedWith(
            compareBy(
                { group -> group.perMatching() },
                // An unfiltered fixed-count rider always fires, so it goes first and can only enable
                // filtered ones; an unfiltered "that many" rider counts everything, so it goes last.
                { group -> group.unfiltered() == group.perMatching() }
            )
        )

        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        val batch = createdTokenIds.toMutableList()

        for (group in bySource) {
            // The batch as this source sees it — its own additions don't feed its other entries.
            val judged = batch.toList()
            val judgedState = newState
            for (active in group) {
                val entityId = active.sourceId
                val effect = active.effect as CreateAdditionalToken
                val event = effect.appliesTo
                if (event !is SdkGameEvent.TokenCreationEvent) continue

                if (!controllerMatches(event.controller, judgedState, active.controllerId, tokenControllerId, predicateEvaluator = predicateEvaluator)) continue

                // How many tokens of the batch match the event's token filter (e.g. "artifact
                // tokens", "a Clue"). A null filter means "any token". Matched against the tokens
                // as they now stand on the battlefield.
                val filter = event.tokenFilter
                val matching = if (filter == null) {
                    judged.size
                } else {
                    judged.count { tokenId ->
                        predicateEvaluator.matches(
                            judgedState, judgedState.projectedState, tokenId, filter,
                            PredicateContext(controllerId = tokenControllerId)
                        )
                    }
                }
                if (matching == 0) continue

                // Extra gates on the rider (CR 614). Evaluated with the *creating* player as the
                // controller — the player the event happens to, matching
                // `ReplacementEffect.restrictions` — and with the rider's own permanent as the
                // source, so a source-relative gate resolves. That is what puts Case of the
                // Pilfered Proof's Clue rider behind its solved designation (CR 702.169b).
                if (effect.restrictions.isNotEmpty()) {
                    val restrictionContext = EffectContext(
                        sourceId = entityId,
                        controllerId = tokenControllerId
                    )
                    val allHold = effect.restrictions.all { restriction ->
                        conditionEvaluator.evaluate(judgedState, restriction, restrictionContext)
                    }
                    if (!allHold) continue
                }

                val cardDef = cardRegistry.getCard(effect.additionalTokenType) ?: continue
                val tapped = effect.inheritTapped && originalTapped

                val addCount = when (effect.scale) {
                    AdditionalTokenScale.PER_EVENT -> effect.additionalTokenCount
                    AdditionalTokenScale.PER_MATCHING_TOKEN ->
                        com.wingedsheep.engine.core.GameLimits.mulClamped(effect.additionalTokenCount, matching)
                }
                repeat(
                    com.wingedsheep.engine.core.GameLimits.cappedTokenCount(addCount, "additional tokens")
                ) {
                    val (tokenId, stateWithId) = newState.newEntity()
                    newState = stateWithId

                    val tokenComponent = CardComponent(
                        cardDefinitionId = effect.additionalTokenType,
                        name = effect.additionalTokenType,
                        manaCost = cardDef.manaCost,
                        typeLine = cardDef.typeLine,
                        baseStats = cardDef.creatureStats,
                        baseKeywords = cardDef.keywords,
                        // The definition's mana cost is the token's (CR 202.1b — none unless defined);
                        // a colored token without one has its color in its color indicator (CR 204),
                        // stored as colorIdentityOverride.
                        colors = cardDef.colorIdentityOverride ?: cardDef.colors,
                        ownerId = tokenControllerId,
                        imageUri = cardDef.metadata.imageUri
                    )

                    var tokenContainer = ComponentContainer.of(
                        tokenComponent,
                        TokenComponent,
                        ControllerComponent(tokenControllerId),
                        SummoningSicknessComponent,
                        EnteredThisTurnComponent
                    )
                    if (tapped) tokenContainer = tokenContainer.with(TappedComponent)
                    if (staticAbilityHandler != null) {
                        tokenContainer = staticAbilityHandler.addContinuousEffectComponent(tokenContainer, cardDef)
                        tokenContainer = staticAbilityHandler.addReplacementEffectComponent(tokenContainer, cardDef)
                    }

                    newState = newState.withEntity(tokenId, tokenContainer)
                    newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
                        .place(newState, tokenControllerId, tokenId)
                    // Honor global "[filter] enter tapped" replacements on the added token too.
                    newState = com.wingedsheep.engine.handlers.effects.EnterTappedReplacements
                        .applyCreatedTokenEntryTap(
                            newState, tokenId, tokenControllerId, definedTapped = tapped,
                            predicateEvaluator = predicateEvaluator
                        )

                    events.add(
                        ZoneChangeEvent(
                            entityId = tokenId,
                            entityName = effect.additionalTokenType,
                            fromZone = null,
                            toZone = Zone.BATTLEFIELD,
                            ownerId = tokenControllerId,
                            oldObject = null,
                            newObject = newState.objectRef(tokenId)
                        )
                    )
                    batch += tokenId
                }
            }
        }

        return newState to events
    }

    /**
     * Find a [ReplaceTokenCreationWithToken] ("if one or more artifact tokens would be created under
     * your control, that many 5/5 red Dragon creature tokens with flying are created instead" —
     * Draconic Visitor) that applies to tokens with [prospectiveCard]'s characteristics being
     * created under [tokenControllerId], and return the substitute token spec.
     *
     * The tokens don't exist yet, so the event's `tokenFilter` is evaluated against a scratch
     * entity carrying the would-be token's [CardComponent] (plus [TokenComponent] and its
     * controller) in a throwaway copy of [state]. The probe isn't on the battlefield, so the
     * predicate evaluator reads its base characteristics — exactly the characteristics the token
     * would be created with.
     *
     * Printed and granted replacements are both consulted ([ActiveReplacements]). The first match
     * wins. The substitute tokens are themselves checked again (CR 616.2: a replacement can become
     * applicable to the event another replacement modified) — Fisher's Talent turns a Fish into a
     * Shark, and its level-3 ability then turns that Shark into an Octopus — but every substitution
     * in [alreadyApplied] is skipped, because a replacement gets only one opportunity to affect an
     * event and the events that replace it (CR 614.5). That is what keeps a substitute that matches
     * its own filter (a second Draconic Visitor's Dragons) from looping.
     *
     * @return the matching substitution, or null when nothing (not already applied) applies.
     */
    fun findTokenSubstitution(
        state: GameState,
        tokenControllerId: EntityId,
        prospectiveCard: CardComponent,
        predicateEvaluator: PredicateEvaluator,
        conditionEvaluator: ConditionEvaluator = predicateEvaluator.conditions,
        alreadyApplied: Set<ActiveReplacement> = emptySet(),
        eligibleSources: Set<EntityId>? = null,
        choices: Map<String, String> = emptyMap()
    ): TokenSubstitutionLookup? {
        val candidates = ActiveReplacements.all(state)
            .filter { it.effect is ReplaceTokenCreationWithToken && it !in alreadyApplied && (eligibleSources == null || it.sourceId in eligibleSources) }
        if (candidates.isEmpty()) return null

        val (probeId, withProbe) = state.newEntity()
        val probeState = withProbe.withEntity(
            probeId,
            ComponentContainer.of(
                prospectiveCard.copy(ownerId = tokenControllerId),
                TokenComponent,
                ControllerComponent(tokenControllerId)
            )
        )

        for (active in candidates) {
            val effect = active.effect as ReplaceTokenCreationWithToken
            val event = effect.appliesTo as? SdkGameEvent.TokenCreationEvent ?: continue
            if (!controllerMatches(event.controller, state, active.controllerId, tokenControllerId, predicateEvaluator = predicateEvaluator)) continue
            val filter = event.tokenFilter
            if (filter != null && !predicateEvaluator.matches(
                    probeState, state.projectedState, probeId, filter,
                    PredicateContext(controllerId = tokenControllerId, sourceId = active.sourceId)
                )
            ) continue
            if (effect.restrictions.isNotEmpty()) {
                val restrictionContext = EffectContext(sourceId = active.sourceId, controllerId = tokenControllerId)
                if (!effect.restrictions.all { conditionEvaluator.evaluate(state, it, restrictionContext) }) continue
            }
            if (!effect.asksForChoice) {
                val token = effect.tokens.single() as? CreateTokenEffect ?: continue
                return TokenSubstitution(active, token)
            }
            val key = substitutionChoiceKey(state, active, tokenControllerId)
            val answer = choices[key] ?: return TokenSubstitutionChoice(active, effect, key)
            val token = answer.toIntOrNull()?.let { effect.tokens.getOrNull(it) } as? CreateTokenEffect
                ?: continue // declined: this replacement doesn't apply to the event
            return TokenSubstitution(active, token)
        }
        return null
    }

    /**
     * Names one [ReplaceTokenCreationWithToken]'s answer for one token controller's creation event
     * in the effect context's chosen values. Built from the source and the replacement's position
     * among that source's replacements, so it survives a paused game being serialized (an effect's
     * hash would not).
     */
    private fun substitutionChoiceKey(state: GameState, active: ActiveReplacement, tokenControllerId: EntityId): String {
        val position = ActiveReplacements.all(state)
            .filter { it.sourceId == active.sourceId && it.granted == active.granted }
            .indexOf(active)
        return "$SUBSTITUTION_CHOICE_PREFIX${active.sourceId.value}:${if (active.granted) "g" else "p"}$position:${tokenControllerId.value}"
    }

    private const val SUBSTITUTION_CHOICE_PREFIX = "__tokenSubstitution:"

    /** The answer recorded for a declined optional substitution. */
    const val SUBSTITUTION_DECLINED = "declined"

    /**
     * Pause [originalEffect] to ask the controller of [choice]'s replacement which substitute
     * tokens to create instead of [tokenCount] tokens (and, when it's optional, whether to). On the
     * answer, [originalEffect] runs again from the top with the answer in its context — nothing has
     * been created yet, so the re-run sees the same event and applies the chosen substitute.
     */
    fun askForSubstitution(
        state: GameState,
        choice: TokenSubstitutionChoice,
        originalEffect: Effect,
        context: EffectContext,
        tokenCount: Int
    ): EffectResult {
        val sourceName = state.getEntity(choice.applied.sourceId)?.get<CardComponent>()?.name ?: "Replacement"
        val substitutes = choice.effect.tokens.map { it as CreateTokenEffect }
        val options = substitutes.map { token ->
            "Create $tokenCount ${ReplaceTokenCreationWithToken.tokenNounPhrase(token).let {
                if (tokenCount == 1) it.replace(" creature tokens", " creature token") else it
            }}"
        } + if (choice.effect.optional) listOf("Create the original tokens") else emptyList()
        val decision = { decisionId: String ->
            com.wingedsheep.engine.core.ChooseOptionDecision(
                id = decisionId,
                playerId = choice.applied.controllerId,
                prompt = if (choice.effect.optional) "Use $sourceName to replace the tokens?"
                else "$sourceName: choose the tokens to create instead",
                context = DecisionContext(
                    sourceId = choice.applied.sourceId,
                    sourceName = sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                options = options
            )
        }
        val continuation = com.wingedsheep.engine.core.TokenSubstitutionChoiceContinuation(
            originalEffect = originalEffect,
            effectContext = context,
            choiceKey = choice.key,
            substituteCount = substitutes.size
        )
        return EffectResult.from(state.suspendForDecision(decision, continuation, emptyList()))
    }

    /**
     * Check if any permanent controlled by the token creator has a
     * ReplaceTokenCreationWithAttachedCopy replacement effect that applies
     * (e.g., Mirrormind Crown, Moonlit Meditation).
     *
     * @return A paused EffectResult if a replacement decision is needed, or null
     */
    fun checkReplacement(
        state: GameState,
        effect: Effect,
        context: EffectContext,
        tokenCount: Int,
        tokenControllerId: EntityId,
        cardRegistry: CardRegistry? = null,
        staticAbilityHandler: StaticAbilityHandler? = null,
        predicateEvaluator: PredicateEvaluator
    ): EffectResult? {
        if (tokenCount <= 0) return null

        val controllerId = tokenControllerId

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val entityController = container.get<ControllerComponent>()?.playerId ?: continue
            if (entityController != controllerId) continue

            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue

            for (re in replacementComponent.replacementEffects) {
                if (re !is ReplaceTokenCreationWithAttachedCopy) continue

                // Check once-per-turn
                if (re.oncePerTurn && container.has<TokenReplacementOfferedThisTurnComponent>()) continue

                // Check the source is attached to something (Aura/Equipment that fell off
                // or never attached can't fire). Attachment-type validation is enforced at
                // cast/attach time by auraTarget / equipmentTarget — no re-check here.
                val attachedTo = container.get<AttachedToComponent>() ?: continue
                val attachedContainer = state.getEntity(attachedTo.targetId) ?: continue
                val attachedCard = attachedContainer.copiableCardComponent() ?: continue

                val cardName = container.get<CardComponent>()?.name ?: "Source"

                // Mark as offered this turn (prevents re-offering on decline)
                var newState = state.withEntity(entityId, container.with(TokenReplacementOfferedThisTurnComponent))

                if (re.optional) {
                    val copyName = attachedCard.name.ifBlank { "the face-down creature" }
                    val prompt = "Use $cardName? Create ${if (tokenCount == 1) "a token that's a copy" else "$tokenCount tokens that are copies"} of $copyName instead?"

                    val decision = { decisionId: String -> YesNoDecision(
                        id = decisionId,
                        playerId = controllerId,
                        prompt = prompt,
                        context = DecisionContext(
                            sourceId = entityId,
                            sourceName = cardName,
                            phase = DecisionPhase.RESOLUTION
                        )
                    ) }

                    val continuation = TokenCreationReplacementContinuation(
                        sourceId = entityId,
                        attachedPermanentId = attachedTo.targetId,
                        originalEffect = effect,
                        tokenCount = tokenCount,
                        effectContext = context
                    )

                    return EffectResult.from(newState.suspendForDecision(decision, continuation, emptyList()))
                } else {
                    // Mandatory replacement — create copies directly
                    return createAttachedPermanentCopies(
                        newState, attachedTo.targetId, controllerId, tokenCount,
                        cardRegistry, staticAbilityHandler,
                        predicateEvaluator = predicateEvaluator
                    )
                }
            }
        }
        return null
    }

    /**
     * Create N token copies of the attached permanent (equipped creature, enchanted
     * permanent, etc.).
     *
     * Per the printed rulings (Mirrormind Crown, Moonlit Meditation), the tokens copy
     * exactly what was printed on the attached permanent. That includes printed
     * "enters with N counters" replacement effects (e.g., Burdened Stoneback's "this
     * creature enters with two -1/-1 counters"), applied via
     * [EntersWithReplacements.applyOnEntry]. Summoning sickness is added
     * only when the copy is itself a creature (CR 302.6).
     */
    fun createAttachedPermanentCopies(
        state: GameState,
        attachedPermanentId: EntityId,
        controllerId: EntityId,
        count: Int,
        cardRegistry: CardRegistry? = null,
        staticAbilityHandler: StaticAbilityHandler? = null,
        predicateEvaluator: PredicateEvaluator
    ): EffectResult {
        val attachedContainer = state.getEntity(attachedPermanentId)
            ?: return EffectResult.success(state)

        val attachedCard = attachedContainer.copiableCardComponent()
            ?: return EffectResult.success(state)

        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        // Same structural cap as CreateTokenExecutor: copies are full entities too.
        val cappedCount = com.wingedsheep.engine.core.GameLimits.cappedTokenCount(count, "token copies")

        repeat(cappedCount) {
            val (tokenId, stateWithId) = newState.newEntity()
            newState = stateWithId
            val tokenCard = attachedCard.copy(ownerId = controllerId, isDoubleFaced = false)

            val components = mutableListOf<Component>(
                tokenCard,
                TokenComponent,
                ControllerComponent(controllerId),
                EnteredThisTurnComponent
            )
            // Summoning sickness only applies to creatures (CR 302.6). An artifact or
            // enchantment token copy doesn't get it; a creature (or artifact-creature)
            // copy does.
            if (tokenCard.typeLine.isCreature) {
                components.add(SummoningSicknessComponent)
            }

            attachedContainer.copiableDoubleFacedComponent {
                it.copy(ownerId = controllerId, isDoubleFaced = false)
            }?.let { components.add(it) }

            var container = ComponentContainer.of(*components.toTypedArray())
            container = CopyExceptionApplier.withNumericKeywords(container, attachedContainer, CopyExceptions.None, cardRegistry)
            if (staticAbilityHandler != null) {
                container = staticAbilityHandler.addContinuousEffectComponent(container)
                container = staticAbilityHandler.addReplacementEffectComponent(container)
            }
            newState = newState.withEntity(tokenId, container)
            newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
                .place(newState, controllerId, tokenId)
            // Honor global "[filter] enter tapped" replacements on the copy too.
            newState = com.wingedsheep.engine.handlers.effects.EnterTappedReplacements
                .applyCreatedTokenEntryTap(newState, tokenId, controllerId, predicateEvaluator = predicateEvaluator)

            // Apply the attached permanent's printed enters-with-counters replacement
            // effects (and any global ones from other permanents).
            if (cardRegistry != null) {
                val (afterCounters, counterEvents) = EntersWithReplacements.applyOnEntry(
                    newState, tokenId, controllerId, cardRegistry,
                    predicateEvaluator = predicateEvaluator
                )
                newState = afterCounters
                events.addAll(counterEvents)

                // CR 306.5b: an Aura can enchant a planeswalker, so a copy of the attached
                // permanent may be one — it enters with the copied printed loyalty (a copiable
                // value, CR 707.2) or state-based actions (CR 704.5i) bin it on arrival.
                val (afterLoyalty, loyaltyEvents) = com.wingedsheep.engine.handlers.effects
                    .ZoneMovementUtils.applyIntrinsicEntryCountersIfNeeded(
                        newState, tokenId, controllerId, cardRegistry,
                        predicateEvaluator = predicateEvaluator
                    )
                newState = afterLoyalty
                events.addAll(loyaltyEvents)
            }

            events.add(
                ZoneChangeEvent(
                    entityId = tokenId,
                    entityName = tokenCard.name,
                    fromZone = null,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = controllerId,
                    oldObject = null,
                    newObject = newState.objectRef(tokenId)
                )
            )
        }

        return EffectResult.success(newState, events)
    }

    private fun List<ActiveReplacement>.perMatching() =
        any { (it.effect as CreateAdditionalToken).scale == AdditionalTokenScale.PER_MATCHING_TOKEN }

    private fun List<ActiveReplacement>.unfiltered() =
        all { ((it.effect as CreateAdditionalToken).appliesTo as? SdkGameEvent.TokenCreationEvent)?.tokenFilter == null }
}
