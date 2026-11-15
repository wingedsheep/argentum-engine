package com.wingedsheep.engine.handlers.effects.permanent.attachments

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.zones.AuraEntry
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.BecomeAuraEffect
import kotlin.reflect.KClass

/**
 * Executor for [BecomeAuraEffect] — "it becomes an Aura with enchant creature. Manifest the top
 * card of your library and attach this enchantment to it" (Cloudform).
 *
 * The source becomes the Aura an [AuraEntry] describes, keeping its own abilities: layer-4 type
 * setting plus a gained enchant ability naming no particular object, for as long as it stays on the
 * battlefield. Then it attaches to [BecomeAuraEffect.attachTo] when it can legally enchant it
 * ([AttachmentMover.canAttach] — the new enchant ability, protection, "can't be enchanted"); with
 * no such object it stays unattached and the unattached-Aura state-based action disposes of it.
 * A no-op when the source has left the battlefield.
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
        if (auraId !in state.getBattlefield()) return EffectResult.success(state)
        val controllerId = state.projectedState.getController(auraId) ?: context.controllerId

        val becameAura = AuraEntry(effect.enchant, losesOtherAbilities = false).apply(state, auraId, controllerId)
        val hostId = context.resolveTarget(effect.attachTo, becameAura)
            ?.takeIf { AttachmentMover.canAttach(becameAura, predicateEvaluator, cardRegistry, auraId, it) }
            ?: return EffectResult.success(becameAura)
        val (attached, events) = AttachmentMover.attach(becameAura, auraId, hostId, controllerId)
        return EffectResult.success(attached, events)
    }
}
