package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Per-call configuration for [CostPaymentService.pay]: what should happen once a payment resolves,
 * plus the context an `onPaid` / `onDeclined` follow-up needs to resolve targets.
 *
 * The follow-ups are serializable [Effect]s so they ride the [com.wingedsheep.engine.core.CostPaymentContinuation]
 * across a pause. That covers the effect-shaped consumers this service is built for — PayOrSuffer's
 * "suffer" ([onDeclined]) and AnyPlayerMayPay's "consequence" ([onPaid]) — and the eventual
 * gated-frame fold. Consumers whose follow-up is engine logic (morph's turn-face-up, chain-copy)
 * instead push their own continuation frame *beneath* the payment continuation; the resumer's
 * `checkForMore` call then resumes it once payment settles.
 *
 * @property onPaid effect to run once the cost is fully paid (null = nothing).
 * @property onDeclined effect to run if the payer declines or cannot pay (null = nothing).
 * @property targets targets carried into the follow-up so an `EffectTarget.ContextTarget(n)` still
 *   resolves (see the engine rule on continuations propagating targets).
 * @property namedTargets named targets from the originating pipeline.
 * @property storedCollections pipeline collections carried into the follow-up so it can reference
 *   cards gathered earlier in the same resolution.
 */
data class CostPaymentContext(
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
    val onPaid: Effect? = null,
    val onDeclined: Effect? = null,
    val targets: List<ChosenTarget> = emptyList(),
    val namedTargets: Map<String, ChosenTarget> = emptyMap(),
    val storedCollections: Map<String, List<EntityId>> = emptyMap()
)
