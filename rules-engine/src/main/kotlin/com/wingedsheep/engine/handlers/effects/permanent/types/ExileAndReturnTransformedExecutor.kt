package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ExileAndReturnTransformedEffect
import com.wingedsheep.sdk.scripting.effects.ReturnFace
import kotlin.reflect.KClass

/**
 * Executor for [ExileAndReturnTransformedEffect] (FIN "Dominant" / eikon transform).
 *
 * Models the "Exile [this], then return it to the battlefield transformed under its owner's
 * control" templating: the target double-faced permanent leaves the battlefield and a brand-new
 * object enters on the requested face. Two zone changes happen atomically inside this single
 * resolution — no priority or state-based actions in between — so the permanent is only momentarily
 * in exile, matching the single-instruction oracle wording.
 *
 * Distinct from [TransformEffectExecutor]: that flips a permanent in place (CR 701.27), preserving
 * counters/damage/attachments and emitting a `TransformedEvent`. This produces a *new object*, so:
 *  - counters/damage drop and attachments fall off (Rule 400.7 "new object" cleanup),
 *  - leaves-the-battlefield triggers fire on the exile and enters-the-battlefield triggers fire on
 *    the return (not transform triggers), and
 *  - a Saga face re-enters with a fresh lore counter via the standard battlefield-entry Saga setup
 *    (CR 714.2b) — see [ZoneTransitionService.moveToZone].
 *
 * The destination face is computed from the on-battlefield face *before* exiling, because a DFC
 * reverts to its front face when it leaves the battlefield (Rule 712.8a).
 */
class ExileAndReturnTransformedExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<ExileAndReturnTransformedEffect> {

    override val effectType: KClass<ExileAndReturnTransformedEffect> =
        ExileAndReturnTransformedEffect::class

    override fun execute(
        state: GameState,
        effect: ExileAndReturnTransformedEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for exile-and-return-transformed")

        val container = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target entity not found")

        // Not a double-faced permanent: there is nothing to transform, so the effect does nothing.
        val dfc = container.get<DoubleFacedComponent>()
            ?: return EffectResult.success(state)

        // Resolve the destination face from the current on-battlefield face up front — the entity
        // reverts to its front face once it leaves the battlefield (Rule 712.8a).
        val destinationFace = when (effect.returnAs) {
            ReturnFace.TRANSFORMED -> when (dfc.currentFace) {
                DoubleFacedComponent.Face.FRONT -> DoubleFacedComponent.Face.BACK
                DoubleFacedComponent.Face.BACK -> DoubleFacedComponent.Face.FRONT
            }
            ReturnFace.FRONT -> DoubleFacedComponent.Face.FRONT
            ReturnFace.BACK -> DoubleFacedComponent.Face.BACK
        }

        // 1. Exile from the battlefield. The permanent ceases to exist as its current object;
        //    leaves-the-battlefield triggers fire and attachments come off via standard cleanup.
        val exileTransition = ZoneTransitionService.moveToZone(state, targetId, Zone.EXILE)
        var newState = exileTransition.state
        val events = exileTransition.events.toMutableList()

        // A replacement effect may have redirected the exile elsewhere; if the entity is no longer
        // reachable, stop after the move rather than fabricating a return.
        if (newState.getEntity(targetId) == null) {
            return EffectResult.success(newState, events)
        }

        // 2. Flip to the destination face and return it to the battlefield as a new object.
        val returnTransition = returnDfcFace(newState, cardRegistry, targetId, destinationFace)
        newState = returnTransition.state
        events.addAll(returnTransition.events)

        return EffectResult.success(newState, events)
    }
}
