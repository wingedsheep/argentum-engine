package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GrantedReplacementEffect
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.scripting.effects.GrantReplacementEffectEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantReplacementEffectEffect].
 * "<permanent> gains '<replacement effect>' until end of turn"
 *
 * Records the replacement into [GameState.grantedReplacementEffects], anchored to the target
 * permanent and tagged with its controller. The zone-change redirect read path
 * ([com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.checkZoneChangeRedirect]) consults
 * granted [com.wingedsheep.sdk.scripting.RedirectZoneChange] entries alongside permanents'
 * printed replacement effects. Mirrors [GrantStaticAbilityExecutor].
 */
class GrantReplacementEffectExecutor : EffectExecutor<GrantReplacementEffectEffect> {

    override val effectType: KClass<GrantReplacementEffectEffect> =
        GrantReplacementEffectEffect::class

    override fun execute(
        state: GameState,
        effect: GrantReplacementEffectEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for replacement effect grant")

        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target no longer exists")
        targetContainer.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")

        val onBattlefield = state.getBattlefield().contains(targetId)
        // A resolving instant/sorcery (its `EffectTarget.Self` is the spell on the stack, not a
        // battlefield permanent) may grant a *floating, controller-scoped* global replacement —
        // Malicious Eclipse's "if a creature an opponent controls would die this turn, exile it
        // instead". The zone-change redirect read path uses only the grant's controllerId + the
        // replacement's filter (never its entityId), so anchoring to the caster is sufficient and the
        // grant persists after the spell leaves. A grant aimed at a permanent that has left the
        // battlefield is still meaningless, so only the source-spell case is allowed off-battlefield.
        if (!onBattlefield && targetId != context.sourceId) {
            return EffectResult.error(state, "Target is not on the battlefield")
        }

        val controllerId = if (onBattlefield) {
            state.projectedState.getController(targetId)
                ?: targetContainer.get<ControllerComponent>()?.playerId ?: context.controllerId
        } else {
            context.controllerId
        }

        val grant = GrantedReplacementEffect(
            entityId = targetId,
            controllerId = controllerId,
            replacement = effect.replacement,
            duration = effect.duration
        )

        val newState = state.copy(
            grantedReplacementEffects = state.grantedReplacementEffects + grant
        )

        return EffectResult.success(newState)
    }
}
