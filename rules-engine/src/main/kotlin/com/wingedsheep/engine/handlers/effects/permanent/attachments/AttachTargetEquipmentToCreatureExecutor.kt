package com.wingedsheep.engine.handlers.effects.permanent.attachments

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.AttachTargetEquipmentToCreatureEffect
import kotlin.reflect.KClass

/**
 * Executor for [AttachTargetEquipmentToCreatureEffect].
 * Attaches a targeted Equipment to a targeted creature.
 * Both the Equipment and creature are explicit targets (not the source).
 *
 * Either target may be declared optional ("up to one target"). When either resolves to nothing —
 * the player declined an optional target, or a required target became illegal before resolution
 * (CR 608.2c) — there is nothing to attach, so the effect is a graceful no-op (Raubahn, Bull of
 * Ala Mhigo attaches "up to one target Equipment"; Blacksmith's Talent attaches to "up to one
 * target creature").
 *
 * An attachment that can't legally be attached to the creature doesn't move, and re-attaching it to
 * the creature it is already on does nothing (both CR 701.3b) — see [AttachmentMover].
 */
class AttachTargetEquipmentToCreatureExecutor(
    private val predicateEvaluator: PredicateEvaluator,
    private val cardRegistry: CardRegistry
) : EffectExecutor<AttachTargetEquipmentToCreatureEffect> {

    override val effectType: KClass<AttachTargetEquipmentToCreatureEffect> =
        AttachTargetEquipmentToCreatureEffect::class

    override fun execute(
        state: GameState,
        effect: AttachTargetEquipmentToCreatureEffect,
        context: EffectContext
    ): EffectResult {
        // "up to one" / fizzled target — nothing to attach, so this is a no-op (not an error).
        val equipmentId = context.resolveTarget(effect.equipmentTarget, state)
            ?: return EffectResult.success(state)

        val creatureId = context.resolveTarget(effect.creatureTarget, state)
            ?: return EffectResult.success(state)

        if (!AttachmentMover.canAttach(state, predicateEvaluator, cardRegistry, equipmentId, creatureId)) {
            return EffectResult.success(state)
        }
        val (newState, events) = AttachmentMover.attach(state, equipmentId, creatureId, context.controllerId)
        return EffectResult.success(newState, events)
    }
}
