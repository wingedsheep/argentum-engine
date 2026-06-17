package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TokenCreationReplacementContinuation
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EntersWithCountersHelper
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
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
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CreateAdditionalToken
import com.wingedsheep.sdk.scripting.DoubleTokenCreation
import com.wingedsheep.sdk.scripting.EventPattern as SdkGameEvent
import com.wingedsheep.sdk.scripting.ModifyTokenCount
import com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithAttachedCopy
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.events.ControllerFilter
import java.util.UUID

/**
 * Checks for token creation replacement effects (e.g., Mirrormind Crown)
 * before tokens are created. If a replacement applies, returns a paused
 * EffectResult with a yes/no decision; otherwise returns null.
 */
object TokenCreationReplacementHelper {

    /**
     * Apply token-count replacement effects whose [SdkGameEvent.TokenCreationEvent] filter
     * matches the player receiving the tokens:
     * - [DoubleTokenCreation] (Anointed Procession / Exalted Sunborn) multiplies the count
     *   by 2 per source; stacks multiplicatively when several apply.
     * - [ModifyTokenCount] shifts the count by a fixed amount per source (clamped at zero).
     *
     * Dispatch reads `appliesTo.controller`, mirroring [ReplacementEffectUtils.applyCounterPlacementModifiers]:
     * `You` matches when the replacement source's controller is the player receiving the
     * tokens, `Opponent` matches when it isn't, `Any` always matches.
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
        baseCount: Int
    ): Int {
        if (baseCount <= 0) return baseCount

        var doublings = 0
        var modifier = 0
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val sourceController = state.projectedState.getController(entityId)
                ?: container.get<ControllerComponent>()?.playerId
                ?: continue
            val repl = container.get<ReplacementEffectSourceComponent>() ?: continue
            for (effect in repl.replacementEffects) {
                val event = when (effect) {
                    is DoubleTokenCreation -> effect.appliesTo
                    is ModifyTokenCount -> effect.appliesTo
                    else -> continue
                }
                if (event !is SdkGameEvent.TokenCreationEvent) continue
                // tokenFilter would need a synthetic token-template snapshot to match
                // against; no card uses it yet, so we conservatively skip filtered events
                // rather than treating them as match-all.
                if (event.tokenFilter != null) continue
                val controllerMatches = when (event.controller) {
                    is ControllerFilter.You -> sourceController == tokenControllerId
                    is ControllerFilter.Opponent -> sourceController != tokenControllerId
                    is ControllerFilter.Any -> true
                }
                if (!controllerMatches) continue
                when (effect) {
                    is DoubleTokenCreation -> doublings += 1
                    is ModifyTokenCount -> modifier += effect.modifier
                }
            }
        }

        // Saturating arithmetic: a stack of doublers (Doubling Season + Anointed Procession + …)
        // multiplies geometrically, which would overflow `Int` to a negative count. Clamp instead
        // (GameLimits); CreateTokenExecutor applies the structural MAX_TOKENS_PER_EFFECT cap before
        // actually allocating entities.
        var count = com.wingedsheep.engine.core.GameLimits.addClamped(baseCount, modifier)
        if (count <= 0) return 0
        repeat(doublings) { count = com.wingedsheep.engine.core.GameLimits.mulClamped(count, 2) }
        return count
    }

    /**
     * Apply [CreateAdditionalToken] replacement effects after a batch of tokens has been
     * created. Models Worldwalker Helm: "If you would create one or more artifact tokens,
     * instead create those tokens plus an additional Map token."
     *
     * Called by the token-creation executors *after* they place their batch, passing the
     * IDs of the tokens just created and the player who created them. For each battlefield
     * permanent with a matching [CreateAdditionalToken] whose `appliesTo` controller filter
     * matches and whose `tokenFilter` (if any) matches at least one of [createdTokenIds],
     * the additional predefined tokens are created once.
     *
     * The additional tokens are placed directly (no further replacement check) so the added
     * artifact Map token cannot recursively re-trigger the same effect — only the original
     * [createdTokenIds] are considered for the filter match.
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
        predicateEvaluator: PredicateEvaluator = PredicateEvaluator()
    ): Pair<GameState, List<com.wingedsheep.engine.core.GameEvent>> {
        if (createdTokenIds.isEmpty() || cardRegistry == null) return state to emptyList()

        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val sourceController = state.projectedState.getController(entityId)
                ?: container.get<ControllerComponent>()?.playerId
                ?: continue
            val repl = container.get<ReplacementEffectSourceComponent>() ?: continue
            for (effect in repl.replacementEffects) {
                if (effect !is CreateAdditionalToken) continue
                val event = effect.appliesTo
                if (event !is SdkGameEvent.TokenCreationEvent) continue

                val controllerMatches = when (event.controller) {
                    is ControllerFilter.You -> sourceController == tokenControllerId
                    is ControllerFilter.Opponent -> sourceController != tokenControllerId
                    is ControllerFilter.Any -> true
                }
                if (!controllerMatches) continue

                // The replacement applies only if at least one of the just-created tokens
                // matches the event's token filter (e.g. "artifact tokens"). A null filter
                // means "any token". Match against base+projected state of the created tokens.
                val filter = event.tokenFilter
                val anyMatch = if (filter == null) {
                    true
                } else {
                    createdTokenIds.any { tokenId ->
                        predicateEvaluator.matches(
                            state, state.projectedState, tokenId, filter,
                            PredicateContext(controllerId = tokenControllerId)
                        )
                    }
                }
                if (!anyMatch) continue

                val cardDef = cardRegistry.getCard(effect.additionalTokenType) ?: continue
                val tapped = effect.inheritTapped && originalTapped

                repeat(
                    com.wingedsheep.engine.core.GameLimits.cappedTokenCount(
                        effect.additionalTokenCount, "additional tokens"
                    )
                ) {
                    val (tokenId, stateWithId) = newState.newEntity()
                    newState = stateWithId

                    val tokenComponent = CardComponent(
                        cardDefinitionId = effect.additionalTokenType,
                        name = effect.additionalTokenType,
                        manaCost = ManaCost.ZERO,
                        typeLine = cardDef.typeLine,
                        baseStats = cardDef.creatureStats,
                        baseKeywords = cardDef.keywords,
                        colors = cardDef.colors,
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

                    events.add(
                        ZoneChangeEvent(
                            entityId = tokenId,
                            entityName = effect.additionalTokenType,
                            fromZone = null,
                            toZone = Zone.BATTLEFIELD,
                            ownerId = tokenControllerId
                        )
                    )
                }
            }
        }

        return newState to events
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
        staticAbilityHandler: StaticAbilityHandler? = null
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
                val attachedCard = attachedContainer.get<CardComponent>() ?: continue

                val cardName = container.get<CardComponent>()?.name ?: "Source"

                // Mark as offered this turn (prevents re-offering on decline)
                var newState = state.withEntity(entityId, container.with(TokenReplacementOfferedThisTurnComponent))

                if (re.optional) {
                    val decisionId = UUID.randomUUID().toString()
                    val prompt = "Use $cardName? Create ${if (tokenCount == 1) "a token that's a copy" else "$tokenCount tokens that are copies"} of ${attachedCard.name} instead?"

                    val decision = YesNoDecision(
                        id = decisionId,
                        playerId = controllerId,
                        prompt = prompt,
                        context = DecisionContext(
                            sourceId = entityId,
                            sourceName = cardName,
                            phase = DecisionPhase.RESOLUTION
                        )
                    )

                    val continuation = TokenCreationReplacementContinuation(
                        decisionId = decisionId,
                        sourceId = entityId,
                        attachedPermanentId = attachedTo.targetId,
                        originalEffect = effect,
                        tokenCount = tokenCount,
                        effectContext = context
                    )

                    newState = newState.withPendingDecision(decision)
                    newState = newState.pushContinuation(continuation)

                    return EffectResult.paused(
                        newState,
                        decision,
                        listOf(
                            DecisionRequestedEvent(
                                decisionId = decisionId,
                                playerId = controllerId,
                                decisionType = "YES_NO",
                                prompt = prompt
                            )
                        )
                    )
                } else {
                    // Mandatory replacement — create copies directly
                    return createAttachedPermanentCopies(
                        newState, attachedTo.targetId, controllerId, tokenCount,
                        cardRegistry, staticAbilityHandler
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
     * [EntersWithCountersHelper.applyEntersWithCounters]. Summoning sickness is added
     * only when the copy is itself a creature (CR 302.6).
     */
    fun createAttachedPermanentCopies(
        state: GameState,
        attachedPermanentId: EntityId,
        controllerId: EntityId,
        count: Int,
        cardRegistry: CardRegistry? = null,
        staticAbilityHandler: StaticAbilityHandler? = null
    ): EffectResult {
        val attachedContainer = state.getEntity(attachedPermanentId)
            ?: return EffectResult.success(state)

        val attachedCard = attachedContainer.get<CardComponent>()
            ?: return EffectResult.success(state)

        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        // Same structural cap as CreateTokenExecutor: copies are full entities too.
        val cappedCount = com.wingedsheep.engine.core.GameLimits.cappedTokenCount(count, "token copies")

        repeat(cappedCount) {
            val (tokenId, stateWithId) = newState.newEntity()
            newState = stateWithId
            val tokenCard = attachedCard.copy(ownerId = controllerId)

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

            var container = ComponentContainer.of(*components.toTypedArray())
            if (staticAbilityHandler != null) {
                container = staticAbilityHandler.addContinuousEffectComponent(container)
                container = staticAbilityHandler.addReplacementEffectComponent(container)
            }
            newState = newState.withEntity(tokenId, container)
            newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
                .place(newState, controllerId, tokenId)

            // Apply the attached permanent's printed enters-with-counters replacement
            // effects (and any global ones from other permanents).
            if (cardRegistry != null) {
                val (afterCounters, counterEvents) = EntersWithCountersHelper.applyEntersWithCounters(
                    newState, tokenId, controllerId, cardRegistry
                )
                newState = afterCounters
                events.addAll(counterEvents)
            }

            events.add(
                ZoneChangeEvent(
                    entityId = tokenId,
                    entityName = tokenCard.name,
                    fromZone = null,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = controllerId
                )
            )
        }

        return EffectResult.success(newState, events)
    }
}
