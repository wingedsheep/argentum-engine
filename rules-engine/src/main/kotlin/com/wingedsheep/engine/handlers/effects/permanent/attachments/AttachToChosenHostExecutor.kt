package com.wingedsheep.engine.handlers.effects.permanent.attachments

import com.wingedsheep.engine.core.AttachToChosenHostContinuation
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TargetRequirementInfo
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.AttachToChosenHostEffect
import kotlin.reflect.KClass

/**
 * Executor for [AttachToChosenHostEffect] — "attach target Aura attached to a creature to another
 * creature" (Autumn-Tail, Kitsune Sage).
 *
 * The new host is chosen at resolution, not targeted, so hexproof and shroud don't limit it. The
 * candidates are the permanents matching the effect's host filter, minus the attachment's current
 * host ("another"), narrowed to those it could legally be attached to ([AttachmentMover.canAttach],
 * CR 701.3a). With none, nothing happens and the attachment stays put (CR 701.3b); otherwise the
 * controller picks one and [AttachToChosenHostContinuation] performs the move.
 */
class AttachToChosenHostExecutor(
    private val predicateEvaluator: PredicateEvaluator,
    private val cardRegistry: CardRegistry
) : EffectExecutor<AttachToChosenHostEffect> {

    override val effectType: KClass<AttachToChosenHostEffect> = AttachToChosenHostEffect::class

    override fun execute(
        state: GameState,
        effect: AttachToChosenHostEffect,
        context: EffectContext
    ): EffectResult {
        val attachmentId = context.resolveTarget(effect.attachment, state)
            ?: return EffectResult.success(state)
        if (attachmentId !in state.getBattlefield()) return EffectResult.success(state)

        val currentHost = state.getEntity(attachmentId)?.get<AttachedToComponent>()?.targetId
        val projected = state.projectedState
        val predicateContext = PredicateContext.fromEffectContext(context)
        val legalHosts = state.getBattlefield().filter { hostId ->
            hostId != currentHost &&
                predicateEvaluator.matches(state, projected, hostId, effect.hostFilter, predicateContext) &&
                AttachmentMover.canAttach(state, predicateEvaluator, cardRegistry, attachmentId, hostId)
        }
        if (legalHosts.isEmpty()) return EffectResult.success(state)

        val attachmentName = state.getEntity(attachmentId)?.get<CardComponent>()?.name ?: "it"
        val decision = { decisionId: String -> ChooseTargetsDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Choose what $attachmentName attaches to",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
                phase = DecisionPhase.RESOLUTION
            ),
            targetRequirements = listOf(
                TargetRequirementInfo(
                    index = 0,
                    description = "another ${effect.hostFilter.description}",
                    minTargets = 1,
                    maxTargets = 1
                )
            ),
            legalTargets = mapOf(0 to legalHosts)
        ) }
        val continuation = AttachToChosenHostContinuation(
            attachmentId = attachmentId,
            controllerId = context.controllerId
        )
        return EffectResult.from(state.suspendForDecision(decision, continuation, emptyList()))
    }
}
