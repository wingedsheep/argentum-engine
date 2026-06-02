package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.IfYouDoContinuation
import com.wingedsheep.engine.core.IfYouDoSnapshot
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for [IfYouDoEffect].
 *
 * Wraps execution of [IfYouDoEffect.action] with an outcome probe. Once the action
 * has fully resolved (synchronously, or after any number of paused decisions),
 * [SuccessCriterion] is evaluated against the captured snapshot to decide whether
 * to dispatch [IfYouDoEffect.ifYouDo] or [IfYouDoEffect.ifYouDont].
 *
 * The pause/resume path follows [CompositeEffectExecutor]'s pre-push pattern: a
 * continuation is pushed onto the stack *before* the action runs. If the action
 * completes synchronously the continuation is popped inline; otherwise the
 * auto-resumer (registered in `CoreAutoResumerModule`) picks it up after the
 * action's own continuations have drained.
 */
class IfYouDoEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<IfYouDoEffect> {

    override val effectType: KClass<IfYouDoEffect> = IfYouDoEffect::class

    override fun execute(
        state: GameState,
        effect: IfYouDoEffect,
        context: EffectContext
    ): EffectResult {
        val snapshot = captureSnapshot(state, effect, context)

        val continuation = IfYouDoContinuation(
            decisionId = "pending",
            ifYouDo = effect.ifYouDo,
            ifYouDont = effect.ifYouDont,
            successCriterion = effect.successCriterion,
            snapshot = snapshot,
            effectContext = context
        )
        val stateWithCont = state.pushContinuation(continuation)

        val result = effectExecutor(stateWithCont, effect.action, context)

        if (result.isPaused) {
            // Action paused; leave continuation on stack for the auto-resumer.
            return result
        }

        // Action ran synchronously (success or recoverable error). Pop our
        // pre-pushed continuation and evaluate the outcome inline.
        val (_, stateWithoutCont) = result.state.popContinuation()
        return evaluateAndDispatch(
            state = stateWithoutCont,
            ifYouDo = effect.ifYouDo,
            ifYouDont = effect.ifYouDont,
            criterion = effect.successCriterion,
            snapshot = snapshot,
            effectContext = context,
            priorEvents = result.events
        )
    }

    /**
     * Capture the data the [SuccessCriterion] needs to evaluate the post-action delta.
     *
     * For [SuccessCriterion.Auto], the probe recognizes two zone-move shapes and records
     * the destination zone's pre-execution size:
     * - a terminal pipeline [MoveCollectionEffect] (multi-object moves), and
     * - a terminal single-target [MoveToZoneEffect] whose target is [EffectTarget.Self]
     *   (e.g. "exile this card from your graveyard. If you do, …" — Council's Deliberation).
     *
     * The collection move is checked first so a pipeline that ends in one keeps its existing
     * semantics. Single-target moves with a non-Self target aren't resolvable here without
     * full target resolution, so they (and genuinely non-move actions such as deal-damage /
     * gain-life) yield an empty snapshot and fall through to the criterion's intrinsic
     * evaluation — for [SuccessCriterion.Auto] that means [evaluateAuto]'s fail-open default,
     * which is correct for actions whose "did it happen" isn't a zone delta.
     */
    private fun captureSnapshot(
        state: GameState,
        effect: IfYouDoEffect,
        context: EffectContext
    ): IfYouDoSnapshot {
        if (effect.successCriterion !is SuccessCriterion.Auto) return IfYouDoSnapshot()

        findTerminalMove(effect.action)?.let { move ->
            val destination = move.destination as? CardDestination.ToZone ?: return IfYouDoSnapshot()
            val ownerId = resolvePlayer(destination.player, context) ?: return IfYouDoSnapshot()
            return zoneSnapshot(state, ownerId, destination.zone)
        }

        findTerminalSingleMove(effect.action)?.let { move ->
            // Only the Self target resolves to a concrete moved entity here; the destination
            // zone is owned by that entity's owner (e.g. self-exile from a graveyard lands in
            // that card's owner's exile). Other targets fall through to fail-open as before.
            if (move.target !is EffectTarget.Self) return IfYouDoSnapshot()
            val movedId = context.sourceId ?: return IfYouDoSnapshot()
            val ownerId = state.getEntity(movedId)?.get<OwnerComponent>()?.playerId ?: return IfYouDoSnapshot()
            return zoneSnapshot(state, ownerId, move.destination)
        }

        return IfYouDoSnapshot()
    }

    private fun zoneSnapshot(state: GameState, ownerId: EntityId, zone: Zone): IfYouDoSnapshot =
        IfYouDoSnapshot(
            destinationZoneOwner = ownerId,
            destinationZoneType = zone,
            destinationZonePreSize = state.zones[ZoneKey(ownerId, zone)]?.size ?: 0
        )

    /**
     * Walk the effect tree for the last [MoveCollectionEffect] in execution order.
     * Returns null for shapes the auto-probe doesn't recognize.
     */
    private fun findTerminalMove(effect: Effect): MoveCollectionEffect? = when (effect) {
        is MoveCollectionEffect -> effect
        is CompositeEffect -> effect.effects.asReversed().firstNotNullOfOrNull { findTerminalMove(it) }
        else -> null
    }

    /**
     * Walk the effect tree for the last single-target [MoveToZoneEffect] in execution order.
     * Returns null for shapes the auto-probe doesn't recognize.
     */
    private fun findTerminalSingleMove(effect: Effect): MoveToZoneEffect? = when (effect) {
        is MoveToZoneEffect -> effect
        is CompositeEffect -> effect.effects.asReversed().firstNotNullOfOrNull { findTerminalSingleMove(it) }
        else -> null
    }

    private fun resolvePlayer(player: Player, context: EffectContext): EntityId? = when (player) {
        is Player.You -> context.controllerId
        is Player.Opponent -> context.opponentId
        is Player.TargetOpponent -> context.opponentId
        is Player.TargetPlayer -> context.targets.firstOrNull()?.let { TargetResolutionUtils.run { it.toEntityId() } }
        is Player.ContextPlayer -> context.targets.getOrNull(player.index)?.let { TargetResolutionUtils.run { it.toEntityId() } }
        is Player.TriggeringPlayer -> context.triggeringEntityId
        else -> context.controllerId
    }

    companion object {
        /**
         * Evaluate the criterion against the post-action state and dispatch
         * [ifYouDo] or [ifYouDont]. Shared between the synchronous path in
         * [IfYouDoEffectExecutor.execute] and the auto-resumer that handles
         * paused-action completion.
         */
        fun evaluateAndDispatch(
            state: GameState,
            ifYouDo: Effect,
            ifYouDont: Effect?,
            criterion: SuccessCriterion,
            snapshot: IfYouDoSnapshot,
            effectContext: EffectContext,
            priorEvents: List<com.wingedsheep.engine.core.GameEvent>,
            effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
        ): EffectResult {
            val happened = evaluate(state, criterion, snapshot)
            val branch = if (happened) ifYouDo else ifYouDont
                ?: return EffectResult.success(state, priorEvents)
            val branchResult = effectExecutor(state, branch, effectContext)
            return branchResult.copy(events = priorEvents + branchResult.events)
        }

        /**
         * Did the action accomplish its work, given the snapshot taken before it ran?
         */
        private fun evaluate(
            state: GameState,
            criterion: SuccessCriterion,
            snapshot: IfYouDoSnapshot
        ): Boolean = when (criterion) {
            is SuccessCriterion.Always -> true
            is SuccessCriterion.Auto -> evaluateAuto(state, snapshot)
            is SuccessCriterion.CollectionNonEmpty ->
                // Pipeline storage doesn't propagate up to this level after
                // resume — until that plumbing exists, fall back to Auto's
                // zone-delta probe (set by the executor's snapshot when the
                // action's terminal move is recognized).
                evaluateAuto(state, snapshot)
        }

        private fun evaluateAuto(state: GameState, snapshot: IfYouDoSnapshot): Boolean {
            val owner = snapshot.destinationZoneOwner
            val zone = snapshot.destinationZoneType
            if (owner == null || zone == null) {
                // No zone-move probe was capturable. This is reached only for non-zone-move
                // actions (deal damage, gain/lose life, draw, …) whose "did it happen" isn't a
                // zone-size delta — for those, treating the action as performed (fail open) is
                // the correct default. Zone-move shapes (collection moves and self-target single
                // moves) are probed in captureSnapshot and never land here.
                return true
            }
            val postSize = state.zones[ZoneKey(owner, zone)]?.size ?: 0
            return postSize > snapshot.destinationZonePreSize
        }

        // Convenience entrypoint without effectExecutor for callers that just want the
        // boolean evaluation (the auto-resumer needs to invoke the executor itself).
        fun didItHappen(
            state: GameState,
            criterion: SuccessCriterion,
            snapshot: IfYouDoSnapshot
        ): Boolean = evaluate(state, criterion, snapshot)
    }

    /**
     * Synchronous-path dispatch helper bound to this executor's effectExecutor.
     */
    private fun evaluateAndDispatch(
        state: GameState,
        ifYouDo: Effect,
        ifYouDont: Effect?,
        criterion: SuccessCriterion,
        snapshot: IfYouDoSnapshot,
        effectContext: EffectContext,
        priorEvents: List<com.wingedsheep.engine.core.GameEvent>
    ): EffectResult = evaluateAndDispatch(
        state, ifYouDo, ifYouDont, criterion, snapshot, effectContext, priorEvents, effectExecutor
    )
}
