package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.AttackTaxConfirmationContinuation
import com.wingedsheep.engine.core.BlockTaxConfirmationContinuation
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.GameState

/**
 * Resumes attack / block declarations that paused for the player to confirm a generic
 * mana tax (Propaganda, Ghostly Prison, Windborn Muse, Collective Restraint, Whipgrass
 * Entangler, etc.).
 *
 * On `Yes`, the resumer pays the already-confirmed tax via the corresponding phase
 * manager's `payConfirmedXTax` helper, then commits the declaration via `commitXDeclaration`.
 *
 * On `No`, the resumer returns an error and the player remains in the declare step —
 * matching CR's "if you can't pay, you couldn't have declared that attack" rule by way
 * of letting the player back out and re-declare.
 */
class CombatTaxContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(AttackTaxConfirmationContinuation::class) { state, continuation, response, _ ->
            resumeAttackTaxConfirmation(state, continuation, response)
        },
        resumer(BlockTaxConfirmationContinuation::class) { state, continuation, response, _ ->
            resumeBlockTaxConfirmation(state, continuation, response)
        },
    )

    private fun resumeAttackTaxConfirmation(
        state: GameState,
        continuation: AttackTaxConfirmationContinuation,
        response: DecisionResponse,
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for attack tax confirmation")
        }
        if (!response.choice) {
            // Decline is a clean no-op: the pause happened *before* any mana was tapped
            // and before AttackingComponent was applied, so the state at this point is
            // already the "didn't attack" state. Surfacing this as an error would render
            // a misleading red banner in the client; instead, drop the player back into
            // DECLARE_ATTACKERS so they can declare a different set (or pass).
            return ExecutionResult.success(state)
        }

        val attackPhase = services.combatManager.attackPhase
        val payResult = attackPhase.payConfirmedAttackTax(state, continuation.attackingPlayer, continuation.totalTax)
        if (!payResult.isSuccess) {
            return payResult
        }
        return attackPhase.commitAttackDeclaration(
            state = payResult.newState,
            attackingPlayer = continuation.attackingPlayer,
            attackers = continuation.attackers,
            projected = payResult.newState.projectedState,
            taxEvents = payResult.events,
        )
    }

    private fun resumeBlockTaxConfirmation(
        state: GameState,
        continuation: BlockTaxConfirmationContinuation,
        response: DecisionResponse,
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for block tax confirmation")
        }
        if (!response.choice) {
            // Same no-op semantics as the attack-tax decline: nothing was tapped yet and
            // no BlockingComponent applied, so we just drop the player back into
            // DECLARE_BLOCKERS without surfacing an error.
            return ExecutionResult.success(state)
        }

        val blockPhase = services.combatManager.blockPhase
        val payResult = blockPhase.payConfirmedBlockTax(state, continuation.blockingPlayer, continuation.totalTax)
        if (!payResult.isSuccess) {
            return payResult
        }
        return blockPhase.commitBlockDeclaration(
            state = payResult.newState,
            blockingPlayer = continuation.blockingPlayer,
            blockers = continuation.blockers,
            taxEvents = payResult.events,
        )
    }
}
