package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.IfYouDoContinuation
import com.wingedsheep.engine.core.IfYouDoSnapshot
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
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
     * For [SuccessCriterion.Auto], walk the action's effect tree for a terminal
     * `MoveCollectionEffect` and record its destination zone's pre-execution size.
     * For other criteria (or actions that don't fit), the snapshot is empty and the
     * resumer falls through to the criterion's intrinsic evaluation.
     */
    private fun captureSnapshot(
        state: GameState,
        effect: IfYouDoEffect,
        context: EffectContext
    ): IfYouDoSnapshot {
        if (effect.successCriterion !is SuccessCriterion.Auto) return IfYouDoSnapshot()

        val terminalMove = findTerminalMove(effect.action) ?: return IfYouDoSnapshot()
        val destination = terminalMove.destination as? CardDestination.ToZone ?: return IfYouDoSnapshot()
        val ownerId = resolvePlayer(destination.player, context) ?: return IfYouDoSnapshot()

        return IfYouDoSnapshot(
            destinationZoneOwner = ownerId,
            destinationZoneType = destination.zone,
            destinationZonePreSize = state.zones[ZoneKey(ownerId, destination.zone)]?.size ?: 0
        )
    }

    /**
     * Walk the effect tree for the last [MoveCollectionEffect] in execution order.
     * Returns null for shapes the auto-probe doesn't recognize.
     */
    private fun findTerminalMove(effect: Effect): MoveCollectionEffect? = when (effect) {
        is MoveCollectionEffect -> effect
        is CompositeEffect -> effect.effects.asReversed().firstNotNullOfOrNull { findTerminalMove(it) }
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
                // No probe was capturable — fail open (preserves prior behavior
                // for action shapes not yet supported by Auto inference).
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
