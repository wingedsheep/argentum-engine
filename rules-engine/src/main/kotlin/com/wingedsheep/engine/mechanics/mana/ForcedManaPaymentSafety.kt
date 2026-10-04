package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.activeManaSpendingScope
import com.wingedsheep.engine.state.forcedPlayFor

/** Keeps each accepted manual payment prefix on a proven path to the instructed cast. */
internal class ForcedManaPaymentSafety(private val services: EngineServices) {
    fun rejection(before: GameState, result: ExecutionResult, action: GameAction): String? {
        if (result.error != null || result.state.gameOver ||
            (action !is ActivateAbility && action !is SubmitDecision)) return null
        val payment = before.continuationStack.firstNotNullOfOrNull { frame ->
            val suspension = when (frame) {
                is Suspension -> frame
                is ReopenManaPaymentDecisionContinuation -> frame.suspension
                else -> null
            }
            (suspension?.answer as? ManaActionPaymentContinuation)?.takeIf {
                val cast = it.action as? CastSpell
                cast != null && before.forcedPlayFor(cast.playerId, cast.cardId) != null &&
                    before.activeManaSpendingScope(cast.playerId) != null
            }
        } ?: return null
        val cast = payment.action as CastSpell
        // A completed cast, or a local payment cancellation returning to the instruction, is safe.
        val after = result.state
        if (after.forcedPlayFor(cast.playerId, cast.cardId) == null) return null
        val reopenIndex = after.continuationStack.indexOfFirst { frame ->
            frame is ReopenManaPaymentDecisionContinuation && frame.suspension.answer == payment
        }
        val activePayment = (after.peekContinuation() as? Suspension)?.answer == payment
        if (reopenIndex < 0 && !activePayment) return null
        // The existing payment frame does not capture X color restrictions or a life/mana split.
        // Do not certify a prefix with a weaker price than the eventual spell payment.
        if (payment.lockedCastCost?.hasX == true || payment.cost.hasX ||
            payment.cost.phyrexianSymbols.isNotEmpty())
            return "This mana choice could not be verified for the announced payment; choose automatic payment"
        val planner = ScopedManaActivationPlanner(services)
        val proof = if (reopenIndex < 0) planner.plan(after, cast.playerId, payment.cost, payment.paymentContext)
            else planner.provePaymentPrefix(after, cast.playerId, payment.cost, payment.paymentContext,
                reopenIndex + 1)
        return when (proof) {
            is ScopedManaPlanResult.Found -> null
            ScopedManaPlanResult.Impossible -> "This mana choice would prevent paying for the instructed card; choose another source or answer"
            is ScopedManaPlanResult.Unknown -> "This mana choice could not be verified for the instructed card; choose another source or automatic payment"
        }
    }
}
