package com.wingedsheep.engine.replacement

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.SelfZoneRedirectComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.ReplacementPriorityGroup
import java.util.*
import kotlin.enums.enumEntries

/**
 * Result returned by [ReplacementEffectProcessor.process].
 */
sealed interface ProcessorResult {

    /** No replacement effects match — the original action should proceed unchanged. */
    data object Pass : ProcessorResult

    /**
     * Replacements are still being resolved. The caller should return this
     * paused result to the engine so it waits for player input.
     */
    data class Paused(val state: GameState, val decision: PendingDecision) : ProcessorResult

    /**
     * All matching replacements have been applied. The outcome tells the
     * caller what to do next.
     *
     * @property executionContext Context for executing the replacement effect,
     *   built from floating-shield data when the matched replacement came from
     *   a floating effect. Null for battlefield-originated replacements.
     * @property identity The identity of the replacement effect that was applied,
     *   or null if no single effect identity is associated with the outcome.
     *   Callers use this for lifecycle management (e.g., consuming NextUse shields).
     */
    data class Resolved(
        val state: GameState,
        val outcome: ReplacementOutcome,
        val executionContext: EffectContext? = null,
        val identity: ReplacementEffectIdentity? = null
    ) : ProcessorResult
}

/**
 * Central processor for replacement effects.
 *
 * Gathers all active replacement effects from the battlefield, granted
 * effects, and self-redirect components, matches them against a
 * [PendingGameEvent], and handles the full CR 614-616 resolution pipeline
 * including:
 *
 * - CR 614.5: Only applies each effect once per event chain
 * - CR 616.1: Player choice between competing same-group effects
 * - CR 616.1f: Repeated application until no more effects match
 *
 * This processor is fully domain-agnostic. Each [PendingGameEvent] subtype
 * implements [PendingGameEvent.matches] (event-pattern matching) and
 * [PendingGameEvent.applyReplacement] (outcome production), so the processor
 * never needs to know about draw, damage, life, or any other domain.
 *
 * ## Source scanning
 * [gatherReplacements] scans four sources:
 * 1. **Battlefield permanents** with [ReplacementEffectSourceComponent]
 * 2. **Floating effects** that carry replacement-effect modifications (Words cycle shields and any future floating replacement effects)
 * 3. **Granted replacement effects** in [GameState.grantedReplacementEffects]
 *    — temporary grants like Malicious Eclipse's "exile instead this turn"
 * 4. **Self-redirect components** via [SelfZoneRedirectComponent] on any
 *    entity
 */
class ReplacementEffectProcessor {

    private val conditionEvaluator = ConditionEvaluator()

    /**
     * Process a pending game event through the replacement effect pipeline.
     *
     * @param state Current game state
     * @param event The pending event to check replacements for
     * @param context Optional execution context for condition evaluation
     * @param alreadyApplied Set of identities already applied in this chain
     * @return The processor result
     */
    fun process(
        state: GameState,
        event: PendingGameEvent,
        context: EffectContext? = null,
        alreadyApplied: Set<ReplacementEffectIdentity> = emptySet()
    ): ProcessorResult {
        // Merge the active replacement chain (CR 614.5 — effect identity chain from
        // a parent Replaced outcome) so nested executions don't re-apply effects
        // that were already consumed earlier in the same chain.
        val fullAlreadyApplied = alreadyApplied + (state.activeReplacementChain ?: emptySet())
        return processInternal(state, event, context, fullAlreadyApplied)
    }

    /**
     * Internal recursive loop. Applies one replacement at a time and re-checks
     * for additional matches until none remain, or we need player input.
     *
     * @return [ProcessorResult.Pass] if no replacements matched the current event,
     *         [ProcessorResult.Resolved] if all replacements resolved (carrying the
     *         final outcome), or [ProcessorResult.Paused] if player input is needed.
     */
    private fun processInternal(
        state: GameState,
        event: PendingGameEvent,
        context: EffectContext?,
        alreadyApplied: Set<ReplacementEffectIdentity>
    ): ProcessorResult {
        // 1. Gather all active replacement effects from the battlefield
        val gathered = gatherReplacements(state, event, context)

        // 2. Filter out already-applied effects (CR 614.5)
        val fresh = gathered.filter { it.identity !in alreadyApplied }

        if (fresh.isEmpty()) {
            return ProcessorResult.Pass
        }

        // 3. Separate optional replacements — they need a yes/no prompt before
        //    entering the standard CR 616.1 pipeline.
        val (optional, mandatory) = fresh.partition {
            it.effect.optional
        }
        if (optional.isNotEmpty()) {
            // Present the optional prompt via the event's domain-specific handler.
            // If the event doesn't support optional prompts, treat as mandatory.
            //
            // Known simplification: CR 616.1 puts optional and mandatory effects in one pool and
            // orders the whole pool by priority group, so a mandatory 616.1a self-replacement
            // should be applied before an optional 616.1e one. Prompting optionals first is only
            // observable when both are applicable to the same event, which needs a card that
            // classifies above ANY — nothing does yet.
            return presentOptionalReplacement(state, event, optional.first(), alreadyApplied, context)
        }

        // 4. If only one mandatory match, apply it directly
        if (mandatory.size == 1) {
            return applySingle(state, mandatory[0], event, alreadyApplied)
        }

        // 5. Multiple mandatory matches — group by priority order (CR 616.1a-e)
        val byGroup = mandatory.groupBy { it.effect.priorityGroup }

        for (group in enumEntries<ReplacementPriorityGroup>()) {
            val groupEffects = byGroup[group] ?: continue
            if (groupEffects.isEmpty()) continue

            if (groupEffects.size > 1) {
                // CR 616.1a–e: when multiple effects share the same priority group, the affected
                // player chooses which to apply. Skip the prompt when the choice is unobservable.
                val first = groupEffects.first()
                if (isChoiceUnobservable(groupEffects, first, event, state)) {
                    return applySingle(state, first, event, alreadyApplied)
                }
                // The order is observable — the player must choose (CR 616.1).
                return presentChoice(state, event, groupEffects, alreadyApplied, context)
            }

            // Single effect — auto-apply
            return applySingle(state, groupEffects.first(), event, alreadyApplied)
        }

        // Unreachable: mandatory was non-empty, so at least one group matched.
        error("unreachable: mandatory effects exist but no priority group matched")
    }

    /**
     * Whether presenting a CR 616.1 choice between [groupEffects] would be busywork — the
     * player cannot tell the orderings apart, so any of them is the right answer.
     *
     * Two cases qualify:
     *
     * 1. **Same source, same effect.** Two copies of one ability on a single permanent
     *    (e.g. a Words-cycle card whose ability was granted twice). Nothing downstream can
     *    distinguish them, not even the execution context.
     * 2. **Equal [ReplacementOutcome.Modified] outcomes.** Two Quantum Riddlers each turn the
     *    announced draw into the same number, and CR 616.1f applies the loser on the next pass
     *    anyway, so the totals converge whichever is picked first. Deliberately restricted to
     *    `Modified`: a `Replaced` outcome executes an effect against its own source
     *    (`EffectContext.sourceId`) and a `Consumed` one may burn a specific `NextUse` shield,
     *    so for those the source is observable even when the outcomes compare equal.
     */
    private fun isChoiceUnobservable(
        groupEffects: List<GatheredReplacement>,
        first: GatheredReplacement,
        event: PendingGameEvent,
        state: GameState
    ): Boolean {
        val firstSourceId = first.sourceEntityId(state)
        val sameSourceSameEffect = firstSourceId != null &&
            groupEffects.all {
                it.effect == first.effect &&
                    it.description == first.description &&
                    it.sourceEntityId(state) == firstSourceId
            }
        if (sameSourceSameEffect) return true

        val firstOutcome = event.applyReplacement(first.effect, state)
        if (firstOutcome !is ReplacementOutcome.Modified) return false
        return groupEffects.all { event.applyReplacement(it.effect, state) == firstOutcome }
    }

    /**
     * Apply a single replacement effect and recursively re-check if modified
     * (CR 616.1f — after applying an effect, repeat until no more apply).
     *
     * If the outcome is [ReplacementOutcome.Modified], the modified event is run through the
     * pipeline again. If no further replacements match (returns [ProcessorResult.Pass]), the
     * final modified event is returned as a [ProcessorResult.Resolved] outcome.
     */
    internal fun applySingle(
        state: GameState,
        gathered: GatheredReplacement,
        event: PendingGameEvent,
        alreadyApplied: Set<ReplacementEffectIdentity>
    ): ProcessorResult {
        val outcome = createOutcome(gathered.effect, event, state)

        // Build execution context — prefer floating-shield context (Words cycle),
        // otherwise use the affected player as the controller so the replacement
        // effect (e.g. DrawCardsEffect from Phial of Galadriel) resolves against
        // the player the event affects, not whoever announced the draw.
        val execContext = when (val identity = gathered.identity) {
            is ReplacementEffectIdentity.FloatingIdentity -> {
                buildContextFromShield(state, identity.floatingId, gathered.sourceControllerId)
            }
            else -> EffectContext(
                controllerId = event.affectedPlayerId,
                sourceId = gathered.sourceEntityId(state)
            )
        }

        // Lifecycle management (e.g., NextUse shield consumption) is the caller's
        // responsibility — the processor only computes the outcome and passes the
        // identity through so callers can act on it.

        val updatedAlreadyApplied = alreadyApplied + gathered.identity

        return when (outcome) {
            is ReplacementOutcome.Modified -> {
                // Stamp the updated chain on state so subsequent iterations of the
                // per-card draw loop don't re-apply the same ModifyDrawAmount (CR 614.5).
                val stateWithChain = state.copy(activeReplacementChain = updatedAlreadyApplied)
                val recurseResult = processInternal(
                    stateWithChain, outcome.modifiedEvent, execContext, updatedAlreadyApplied
                )
                when (recurseResult) {
                    is ProcessorResult.Pass -> {
                        ProcessorResult.Resolved(stateWithChain, outcome, execContext, gathered.identity)
                    }
                    else -> recurseResult
                }
            }
            is ReplacementOutcome.Replaced -> {
                // Stamp the updated chain on the returned state so nested effect
                // execution (e.g. a DrawCardsEffect produced by the replacement)
                // does not re-trigger effects already applied in this chain.
                val stateWithChain = state.copy(activeReplacementChain = updatedAlreadyApplied)
                ProcessorResult.Resolved(stateWithChain, outcome, execContext, gathered.identity)
            }
            is ReplacementOutcome.Consumed -> {
                ProcessorResult.Resolved(state, outcome, execContext, gathered.identity)
            }
        }
    }

    /**
     * Build an [EffectContext] from a NextUse floating shield's stored data,
     * so the caller can execute the replacement effect without searching the
     * original state for shield data.
     */
    private fun buildContextFromShield(
        state: GameState,
        floatingId: EntityId,
        controllerId: EntityId
    ): EffectContext? {
        val fe = state.floatingEffects.firstOrNull { it.id == floatingId } ?: return null
        return fe.effect.modification.toEffectContext(controllerId)
    }

    /**
     * Remove a floating effect by its index in [GameState.floatingEffects].
     * Only consumes effects with [Duration.NextUse] — other durations
     * (EndOfTurn, Permanent, etc.) are managed by the turn/phase/condition
     * expiry machinery and must not be removed here.
     */
    internal fun consumeFloatingEffect(state: GameState, floatingId: EntityId): GameState {
        val floatingEffect = state.floatingEffects.firstOrNull { it.id == floatingId }
        if (floatingEffect == null || floatingEffect.duration !is Duration.NextUse) return state
        val updatedEffects = state.floatingEffects.toMutableList()
        updatedEffects.remove(floatingEffect)
        return state.copy(floatingEffects = updatedEffects)
    }

    /**
     * Present a choice between multiple competing replacement effects to
     * the affected player (CR 616.1e).
     */
    private fun presentChoice(
        state: GameState,
        event: PendingGameEvent,
        options: List<GatheredReplacement>,
        alreadyApplied: Set<ReplacementEffectIdentity>,
        context: EffectContext?
    ): ProcessorResult.Paused {
        val playerId = event.affectedPlayerId
        val decisionId = UUID.randomUUID().toString()

        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Choose which replacement effect to apply",
            context = DecisionContext(
                sourceId = context?.sourceId,
                sourceName = "Replacement effect choice",
                phase = DecisionPhase.RESOLUTION
            ),
            options = disambiguate(options.map { it.description }),
            canCancel = false
        )

        val continuation = ReplacementChoiceContinuation(
            decisionId = decisionId,
            pendingEvent = event,
            options = options,
            alreadyApplied = alreadyApplied,
            context = context
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ProcessorResult.Paused(stateWithContinuation, decision)
    }

    /**
     * Present a yes/no prompt for an optional replacement effect
     * (e.g., Parallel Thoughts).
     *
     * Delegates to the event's [PendingGameEvent.createOptionalPrompt] for
     * domain-specific prompt and continuation construction. If the event
     * returns null (no optional prompt support), the effect is treated
     * as mandatory.
     */
    private fun presentOptionalReplacement(
        state: GameState,
        event: PendingGameEvent,
        gathered: GatheredReplacement,
        alreadyApplied: Set<ReplacementEffectIdentity>,
        context: EffectContext?
    ): ProcessorResult {
        val decisionId = UUID.randomUUID().toString()
        val promptResult = event.createOptionalPrompt(decisionId, gathered, state, context)
            ?: // Event doesn't support optional prompts — treat as mandatory
            return applySingle(state, gathered, event, alreadyApplied)

        val stateWithDecision = state.withPendingDecision(promptResult.decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(promptResult.continuation)

        return ProcessorResult.Paused(stateWithContinuation, promptResult.decision)
    }

    /**
     * Create a [ReplacementOutcome] by applying a single replacement effect
     * to a pending event.
     *
     * Delegates to [PendingGameEvent.applyReplacement] for domain-specific
     * outcome production — the processor remains domain-agnostic.
     */
    private fun createOutcome(
        effect: ReplacementEffect,
        event: PendingGameEvent,
        state: GameState
    ): ReplacementOutcome {
        return event.applyReplacement(effect, state)
    }

    /**
     * Gather all active replacement effects that match the given event.
     *
     * Scans four sources:
     * 1. Battlefield permanents with [ReplacementEffectSourceComponent]
     * 2. Floating effects that carry replacement-effect modifications (Words cycle shields and any future floating replacement effects)
     * 3. [state.grantedReplacementEffects] — temporary grants like Malicious Eclipse
     * 4. Entities with [SelfZoneRedirectComponent] — "from anywhere" self-redirects
     *
     * Each effect is matched via [PendingGameEvent.matches], so unsupported
     * event domains are naturally filtered out.
     */
    fun gatherReplacements(
        state: GameState,
        event: PendingGameEvent,
        context: EffectContext? = null
    ): List<GatheredReplacement> {
        val results = mutableListOf<GatheredReplacement>()
        val battlefieldSet = state.getBattlefield().toSet()

        // 1. Battlefield permanents with ReplacementEffectSourceComponent
        for (entityId in battlefieldSet) {
            val container = state.getEntity(entityId) ?: continue
            val replacementSource = container.get<ReplacementEffectSourceComponent>() ?: continue
            // Projected, not base: control change is a layer-2 effect (see EffectApplicator's
            // ChangeController branch), so a stolen permanent's ControllerComponent still names
            // its original controller. A `Player.You` replacement has to follow the permanent.
            val controllerId = state.projectedState.getController(entityId)
                ?: container.get<ControllerComponent>()?.playerId
                ?: continue

            for ((index, effect) in replacementSource.replacementEffects.withIndex()) {
                val evalContext = context ?: EffectContext(
                    controllerId = controllerId,
                    sourceId = entityId
                )
                if (!matchesEvent(effect, event, controllerId, state, evalContext)) continue

                results.add(
                    GatheredReplacement(
                        identity = ReplacementEffectIdentity.BattlefieldIdentity(
                            sourceEntityId = entityId,
                            effectIndex = index
                        ),
                        effect = effect,
                        sourceControllerId = controllerId,
                        description = describe(state, entityId, effect.description)
                    )
                )
            }
        }

        // 2. Floating effects that carry replacement-effect modifications
        // (Words cycle shields stored here, not on battlefield permanents).
        for (fe in state.floatingEffects) {
            if (event.affectedPlayerId !in fe.effect.affectedEntities) continue

            val sdkEffect = fe.effect.modification.toReplacementEffect(fe.controllerId) ?: continue

            if (!matchesEvent(sdkEffect, event, fe.controllerId, state, context)) continue

            val cardName = fe.sourceName
                ?: fe.sourceId
                ?.let { state.getEntity(it)?.get<CardComponent>()?.name }

            results.add(
                GatheredReplacement(
                    identity = ReplacementEffectIdentity.FloatingIdentity(floatingId = fe.id),
                    effect = sdkEffect,
                    sourceControllerId = fe.controllerId,
                    description = prefixSource(cardName, sdkEffect.description)
                )
            )
        }

        // 3. Granted replacement effects (temporary riders like Malicious Eclipse)
        for ((index, grant) in state.grantedReplacementEffects.withIndex()) {
            val controllerId = grant.controllerId
            if (!matchesEvent(grant.replacement, event, controllerId, state, context)) continue

            results.add(
                GatheredReplacement(
                    identity = ReplacementEffectIdentity.GrantedIdentity(grantedIndex = index),
                    effect = grant.replacement,
                    sourceControllerId = controllerId,
                    description = describe(state, grant.entityId, grant.replacement.description)
                )
            )
        }

        // 4. Self-redirect components — "from anywhere" effects on the card entity itself
        //    (Darksteel Colossus, Progenitus). The ability names the zones it works from
        //    ("from anywhere"), so it functions from all of them (CR 113.6b).
        //    Skip entities on the battlefield since their effects are already gathered
        //    via ReplacementEffectSourceComponent above (source 1).
        for ((entityId, container) in state.entities) {
            if (entityId in battlefieldSet) continue
            val selfRedirect = container.get<SelfZoneRedirectComponent>() ?: continue
            // Determine controller: use ControllerComponent if present, else OwnerComponent
            val controllerId = container.get<ControllerComponent>()?.playerId
                ?: container.get<OwnerComponent>()?.playerId
                ?: continue

            for ((index, effect) in selfRedirect.redirects.withIndex()) {
                if (!matchesEvent(effect, event, controllerId, state, context)) continue

                results.add(
                    GatheredReplacement(
                        identity = ReplacementEffectIdentity.SelfRedirectIdentity(
                            sourceEntityId = entityId,
                            effectIndex = index
                        ),
                        effect = effect,
                        sourceControllerId = controllerId,
                        description = describe(state, entityId, effect.description)
                    )
                )
            }
        }

        return results
    }

    /**
     * Name the card an option came from. [GatheredReplacement.description] is player-facing —
     * it is what a `ChooseOptionDecision` lists and what the optional-replacement prompt reads
     * back — so two copies of the same card competing must not render as the same string.
     */
    private fun describe(state: GameState, sourceId: EntityId?, effectDescription: String): String =
        prefixSource(
            sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            effectDescription
        )

    private fun prefixSource(cardName: String?, effectDescription: String): String =
        if (cardName == null) effectDescription else "$cardName - $effectDescription"

    /**
     * Number any options that still read identically after [describe] has named their source —
     * two copies of one card share a name, so the name alone doesn't separate them. Reaching
     * this means [isChoiceUnobservable] judged the order observable, so the player has a real
     * decision to make and needs labels they can actually tell apart.
     */
    private fun disambiguate(descriptions: List<String>): List<String> {
        val counts = descriptions.groupingBy { it }.eachCount()
        val seen = mutableMapOf<String, Int>()
        return descriptions.map { desc ->
            if (counts.getValue(desc) == 1) return@map desc
            val n = seen.merge(desc, 1, Int::plus)!!
            "$desc (#$n)"
        }
    }

    /**
     * Check whether a replacement effect matches a pending event using its
     * [ReplacementEffect.appliesTo] pattern and any [ReplacementEffect.restrictions].
     *
     * Delegates event-pattern matching to [PendingGameEvent.matches], then
     * evaluates the effect's [ReplacementEffect.restrictions] against the
     * current state. This method is fully domain-agnostic.
     */
    private fun matchesEvent(
        effect: ReplacementEffect,
        event: PendingGameEvent,
        sourceControllerId: EntityId,
        state: GameState,
        context: EffectContext? = null
    ): Boolean {
        // Delegate to the polymorphic event match
        if (!event.matches(effect.appliesTo, sourceControllerId, state, context)) {
            return false
        }

        // Evaluate the effect's restrictions (CR 614 — extra conditions).
        // Restrictions are evaluated against the affected player, not against
        // whoever announced the event (e.g. a spell's controller). Conditions
        // like CardsInHandAtMost use Player.You → EffectContext.controllerId
        // to determine whose hand to count, so the controller must be the
        // player the event is happening to, not the caller's context.
        val restrictions = effect.restrictions
        if (restrictions.isNotEmpty()) {
            val evalContext = EffectContext(
                sourceId = null,
                controllerId = event.affectedPlayerId
            )
            return restrictions.all { condition ->
                conditionEvaluator.evaluate(state, condition, evalContext)
            }
        }

        return true
    }
}