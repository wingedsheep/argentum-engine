package com.wingedsheep.engine.handlers.effects.permanent.tapping

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.core.untapOrConsumeStun
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import kotlin.reflect.KClass

/**
 * Executor for TapUntapEffect.
 * "Tap target creature" or "Untap target creature"
 *
 * The untap branch routes through [untapOrConsumeStun] so stun-counter
 * replacement (Rule 122.1d) is handled the same way as the natural untap step.
 */
class TapUntapExecutor : EffectExecutor<TapUntapEffect> {

    override val effectType: KClass<TapUntapEffect> = TapUntapEffect::class

    override fun execute(
        state: GameState,
        effect: TapUntapEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for tap/untap")

        if (!effect.tap) {
            // Explicit "untap target" effect — pass projected = null so the
            // untap-step-only REMOVE_COUNTER_TO_UNTAP replacement does not apply.
            val (newState, events) = untapOrConsumeStun(state, targetId)
            return EffectResult.success(newState, events)
        }

        // The tap atom owns the already-tapped no-op guard (CR 603.2f) and the TappedEvent.
        // The tapper is this effect's controller — the player the game instructs to tap, which
        // "whenever you tap a creature an opponent controls" reads. Inside a per-player loop
        // (Tangle Wire's "that player taps …") `controllerId` is already rebound to the player
        // doing the tapping, so their tap is correctly attributed to them and not to the card's
        // controller.
        val (newState, event) = tap(state, targetId, tappedById = context.controllerId)
        return EffectResult.success(newState, listOfNotNull(event))
    }
}
