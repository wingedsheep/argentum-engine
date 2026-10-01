package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.CreateTokenCopyAuraHostContinuation
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TargetRequirementInfo
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.copiableCardComponent
import com.wingedsheep.engine.handlers.effects.copy.CopyExceptionApplier
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect

/**
 * Raises the "what does this Aura token enchant?" choice (CR 303.4f).
 *
 * A token copy of an Aura is put onto the battlefield without being cast, so it never targets.
 * Instead its controller chooses what it enchants as it enters, restricted to objects the copied
 * Aura could legally enchant (CR 303.4f — its printed `enchant` restriction, with targeting
 * restrictions such as hexproof and shroud ignored, since nothing is being targeted).
 *
 * The choice is raised *before* the token is created so it enters already attached: its
 * enters-the-battlefield triggers and the first state-based check both see a properly attached
 * Aura. If no legal object exists the token isn't created at all (CR 303.4g).
 *
 * Each token gets its own choice, so an effect creating several Aura copies asks once per token —
 * the continuation carries how many are still owed.
 */
internal object AuraTokenHostChooser {

    /**
     * Pause for the controller to pick a host for the next Aura token, or return an unchanged
     * state when there is nothing the Aura could legally enchant (no token is created).
     */
    fun pause(
        state: GameState,
        effect: CreateTokenCopyOfTargetEffect,
        context: EffectContext,
        auraDefinitionId: String,
        auraName: String,
        controllerId: EntityId,
        remaining: Int,
        cardRegistry: CardRegistry?,
        targetFinder: TargetFinder
    ): EffectResult {
        if (remaining <= 0) return EffectResult.success(state)

        val hosts = legalHosts(state, effect, context, auraDefinitionId, controllerId, cardRegistry, targetFinder = targetFinder)
        if (hosts.isEmpty()) {
            // Nothing legal to enchant — the Aura token can't enter (CR 303.4g), and neither can
            // any of the ones still owed, since they would all copy the same Aura.
            return EffectResult.success(state)
        }

        val decision = { decisionId: String -> ChooseTargetsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose what the $auraName token enchants",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
                phase = DecisionPhase.RESOLUTION,
            ),
            targetRequirements = listOf(
                TargetRequirementInfo(
                    index = 0,
                    description = "permanent for the $auraName token to enchant",
                    minTargets = 1,
                    maxTargets = 1,
                )
            ),
            legalTargets = mapOf(0 to hosts),
        ) }

        val continuation = CreateTokenCopyAuraHostContinuation(
            effect = effect,
            context = context,
            controllerId = controllerId,
            auraDefinitionId = auraDefinitionId,
            auraName = auraName,
            remaining = remaining,
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation, emptyList()))
    }

    /**
     * Objects the copied Aura could legally enchant. Derived from the Aura card definition's
     * `auraTarget` — the token copies the printed enchant restriction along with everything else.
     * An Aura whose definition declares no enchant restriction has no legal host.
     */
    private fun legalHosts(
        state: GameState,
        effect: CreateTokenCopyOfTargetEffect,
        context: EffectContext,
        auraDefinitionId: String,
        controllerId: EntityId,
        cardRegistry: CardRegistry?,
        targetFinder: TargetFinder
    ): List<EntityId> {
        val auraTarget = cardRegistry?.getCard(auraDefinitionId)?.script?.auraTarget
            ?: return emptyList()
        val originalId = context.resolveTarget(effect.target, state) ?: return emptyList()
        val original = state.getEntity(originalId)?.copiableCardComponent() ?: return emptyList()
        val prospective = CopyExceptionApplier.apply(original, effect.copyExceptions).copy(ownerId = controllerId)
        // A copy is a different object: it never inherits the original Aura's source exception.
        val (previewId, allocated) = state.newEntity()
        val preview = allocated.withEntity(previewId, ComponentContainer.of(prospective, ControllerComponent(controllerId)))
        return targetFinder.findLegalTargets(
            state = preview,
            requirement = auraTarget,
            controllerId = controllerId,
            sourceId = previewId,
            ignoreTargetingRestrictions = true,
        )
    }
}
