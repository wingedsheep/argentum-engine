package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantActivatedAbilityEffect.
 * "Target [permanent] gains '[activated ability]' until end of turn"
 *
 * Adds the activated ability to GameState.grantedActivatedAbilities,
 * where GameSession will find it when computing legal actions and
 * ActivateAbilityHandler will find it when validating activations.
 *
 * Target-general: the grant works on any battlefield permanent, not just creatures
 * (e.g. Glorious Sunrise grants a land "{T}: Add {G}{G}{G}"). The type of a valid
 * target is already constrained by the effect's [GrantActivatedAbilityEffect.target]
 * requirement, so the executor only verifies the resolved target is a permanent on the
 * battlefield or a permanent spell on the stack ([ObjectGrantTarget]).
 */
class GrantActivatedAbilityExecutor : EffectExecutor<GrantActivatedAbilityEffect> {

    override val effectType: KClass<GrantActivatedAbilityEffect> =
        GrantActivatedAbilityEffect::class

    override fun execute(
        state: GameState,
        effect: GrantActivatedAbilityEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for activated ability grant")

        // Verify target exists and is a permanent or a permanent spell.
        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target no longer exists")
        targetContainer.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")
        if (!ObjectGrantTarget.canReceive(state, targetId)) {
            return EffectResult.error(state, ObjectGrantTarget.NOT_A_PERMANENT_OR_PERMANENT_SPELL)
        }

        val grant = GrantedActivatedAbility(
            entityId = targetId,
            ability = effect.ability,
            duration = effect.duration,
            sourceId = context.sourceId,
            sourceObject = exiledSourceObject(state, effect, context)
        )

        val newState = state.copy(
            grantedActivatedAbilities = state.grantedActivatedAbilities + grant
        )

        return EffectResult.success(newState)
    }

    /**
     * For [Duration.UntilSourceCastFromExile], the source card's current exile object — the one
     * whose cast ends the grant (see `SpellCaster`). Null when the source is not in exile right now:
     * "this card" can then never be cast from exile as the object the effect names, so the grant
     * lasts indefinitely (CR 400.7).
     */
    private fun exiledSourceObject(
        state: GameState,
        effect: GrantActivatedAbilityEffect,
        context: EffectContext
    ): ObjectRef? {
        if (effect.duration != Duration.UntilSourceCastFromExile) return null
        val sourceId = context.sourceId ?: return null
        val inExile = state.turnOrder.any { sourceId in state.getExile(it) }
        return if (inExile) state.objectRef(sourceId) else null
    }
}
