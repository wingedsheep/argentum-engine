package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.mechanics.daynight.DayNightService
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Keyword

/**
 * The daybound/nightbound designation-start and transform-reconciliation checks (CR 702.145c/d/f/g).
 *
 * These aren't loss checks; they're the "any time a player controls a permanent with daybound/
 * nightbound …" continuous conditions of CR 702.145, which the rules describe as happening
 * immediately rather than as true state-based actions. This engine polls them on the SBA cadence
 * because the SBA sweep is exactly the "runs to fixpoint whenever a player would get priority" hook
 * they need, and — critically — the priority-time SBA call sites feed their emitted events to
 * `TriggerDetector.detectTriggers`, so a `TransformedEvent` produced here fires "whenever this
 * transforms" triggers. (The untap-step designation change of CR 502.2 is handled separately, in the
 * untap turn-based action itself, for the same trigger-visibility reason.)
 *
 * Two responsibilities, both delegated to [DayNightService] so the single-writer invariant holds:
 *
 *  - **Designation start** (CR 702.145d/g): if it's neither day nor night and a player controls a
 *    daybound permanent, it becomes day; else if a player controls a nightbound permanent and there
 *    are no daybound permanents anywhere on the battlefield, it becomes night. Daybound wins when both
 *    are present (702.145g's "and there are no permanents with daybound on the battlefield" gate).
 *  - **Transform reconciliation** (CR 702.145c/f): once a designation holds, a permanent that arrives
 *    front-face-up-daybound during night (or back-face-up-nightbound during day) is transformed. This
 *    is [DayNightService.applyTransformCascade], which is idempotent and covers permanents that enter
 *    or are granted the keyword after the designation was set.
 *
 * Registered by [PermanentSbaModule], which already receives the [CardRegistry] the transform cascade
 * needs.
 */
class DayNightCheck(private val cardRegistry: CardRegistry) : StateBasedActionCheck {
    override val name = "702.145 Daybound/Nightbound"
    override val order = SbaOrder.DAY_NIGHT

    override fun check(state: GameState): ExecutionResult {
        // Designation start (CR 702.145d/g) only applies while it's neither day nor night.
        if (state.dayNight == null) {
            val projected = state.projectedState
            var anyDaybound = false
            var anyNightbound = false
            for (entityId in state.getBattlefield()) {
                if (projected.hasKeyword(entityId, Keyword.DAYBOUND)) anyDaybound = true
                if (projected.hasKeyword(entityId, Keyword.NIGHTBOUND)) anyNightbound = true
                if (anyDaybound) break // daybound wins outright; no need to keep scanning
            }

            val (newState, events) = when {
                anyDaybound -> DayNightService.becomeDay(state, cardRegistry, Keyword.DAYBOUND.displayName)
                anyNightbound -> DayNightService.becomeNight(state, cardRegistry, Keyword.NIGHTBOUND.displayName)
                else -> state to emptyList<GameEvent>()
            }
            return ExecutionResult.success(newState, events)
        }

        // A designation already holds: reconcile any front/back face that's out of step with it
        // (CR 702.145c/f) — e.g. a daybound creature that just entered while it's night.
        val (newState, events) = DayNightService.applyTransformCascade(state, cardRegistry)
        return ExecutionResult.success(newState, events)
    }
}
