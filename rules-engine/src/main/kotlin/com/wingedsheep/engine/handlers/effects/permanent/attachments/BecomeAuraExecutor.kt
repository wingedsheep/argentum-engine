package com.wingedsheep.engine.handlers.effects.permanent.attachments

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffects
import com.wingedsheep.engine.mechanics.layers.createFloatingEffect
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.GainedEnchantRestrictionComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.BecomeAuraEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for [BecomeAuraEffect] — "it becomes an Aura with enchant creature. Manifest the top
 * card of your library and attach this enchantment to it" (Cloudform).
 *
 * "Becomes an Aura" changes only the object's enchantment types (CR 205.1a: a new subtype replaces
 * the existing subtypes *from the same set*): layer 4 adds Aura and removes any other enchantment
 * type it has, while its card types, other subtypes and abilities stay — an animated Form is a
 * creature Aura, which the unattached-Aura state-based action puts into the graveyard (CR 303.4d).
 * Its whole enchant ability becomes [BecomeAuraEffect.enchant], naming no particular object. Both
 * last for as long as it stays on the battlefield. Then it attaches to [BecomeAuraEffect.attachTo]
 * when it can legally enchant it ([AttachmentMover.canAttach] — the new enchant ability,
 * protection, "can't be enchanted"); with no such object it stays unattached and the same
 * state-based action disposes of it.
 *
 * A no-op when the source is no longer the object whose ability this is — it left the battlefield,
 * or left and came back as a new object (CR 400.7).
 */
class BecomeAuraExecutor(
    private val predicateEvaluator: PredicateEvaluator,
    private val cardRegistry: CardRegistry
) : EffectExecutor<BecomeAuraEffect> {

    override val effectType: KClass<BecomeAuraEffect> = BecomeAuraEffect::class

    override fun execute(
        state: GameState,
        effect: BecomeAuraEffect,
        context: EffectContext
    ): EffectResult {
        val auraId = context.sourceId ?: return EffectResult.success(state)
        if (auraId !in state.getBattlefield() || context.isUnavailableBattlefieldSource(EffectTarget.Self, state)) {
            return EffectResult.success(state)
        }
        val controllerId = state.projectedState.getController(auraId) ?: context.controllerId

        val becameAura = becomeAura(state, auraId, controllerId, effect)
        val hostId = context.resolveTarget(effect.attachTo, becameAura)
            ?.takeIf { AttachmentMover.canAttach(becameAura, predicateEvaluator, cardRegistry, auraId, it) }
            ?: return EffectResult.success(becameAura)
        val (attached, events) = AttachmentMover.attach(becameAura, auraId, hostId, controllerId)
        return EffectResult.success(attached, events)
    }

    private fun becomeAura(state: GameState, auraId: EntityId, controllerId: EntityId, effect: BecomeAuraEffect): GameState {
        val context = EffectContext(sourceId = auraId, controllerId = controllerId)
        var newState = state.updateEntity(auraId) { it.with(GainedEnchantRestrictionComponent(filter = effect.enchant)) }
        val otherEnchantmentTypes = state.projectedState.getSubtypes(auraId)
            .filter { it in Subtype.ALL_ENCHANTMENT_TYPES && it != AURA }
        val modifications = otherEnchantmentTypes.map { SerializableModification.RemoveSubtype(it) } +
            SerializableModification.AddSubtype(AURA)
        val retype = modifications.map { modification ->
            val (id, withId) = newState.newEntity()
            newState = withId
            newState.createFloatingEffect(Layer.TYPE, modification, setOf(auraId), Duration.Permanent, context, id = id)
        }
        return newState.addFloatingEffects(retype).tick()
    }

    private companion object {
        const val AURA = "Aura"
    }
}
