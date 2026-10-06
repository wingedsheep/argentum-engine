package com.wingedsheep.engine.handlers.effects.permanent.attachments

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.GainedEnchantRestrictionComponent
import com.wingedsheep.sdk.scripting.effects.EnchantPutOntoBattlefieldEffect
import kotlin.reflect.KClass

/**
 * Executor for [EnchantPutOntoBattlefieldEffect] — "it loses 'enchant creature card in a graveyard'
 * and gains 'enchant creature put onto the battlefield with this Aura.' … attach this Aura to it"
 * (Animate Dead).
 *
 * The source Aura's enchant ability is replaced by a [GainedEnchantRestrictionComponent] naming the
 * objects in the [EnchantPutOntoBattlefieldEffect.from] collection that are on the battlefield now,
 * each pinned to its current object identity (CR 400.7). That happens whether or not anything can be
 * attached: the printed ability is lost either way, so an Aura still attached to the card it
 * enchanted (the return was replaced or prevented) is now illegally attached and the CR 704.5m
 * state-based action puts it into its owner's graveyard. Then the Aura attaches to the first
 * recorded object it can legally enchant (CR 701.3a — its new enchant ability, protection from its
 * colors, "can't be enchanted"); with none it doesn't move (CR 701.3b) and the same state-based
 * action disposes of it.
 *
 * A no-op when the source is no longer on the battlefield (the Animate Dead ruling: "If Animate
 * Dead isn't on the battlefield as its triggered ability resolves, none of its effects happen").
 */
class EnchantPutOntoBattlefieldExecutor(
    private val predicateEvaluator: PredicateEvaluator,
    private val cardRegistry: CardRegistry
) : EffectExecutor<EnchantPutOntoBattlefieldEffect> {

    override val effectType: KClass<EnchantPutOntoBattlefieldEffect> = EnchantPutOntoBattlefieldEffect::class

    override fun execute(
        state: GameState,
        effect: EnchantPutOntoBattlefieldEffect,
        context: EffectContext
    ): EffectResult {
        val auraId = context.sourceId ?: return EffectResult.success(state)
        val battlefield = state.getBattlefield()
        if (auraId !in battlefield) return EffectResult.success(state)

        val hosts = context.pipeline.storedCollections[effect.from].orEmpty()
            .filter { it in battlefield && it != auraId }
            .distinct()
        val refs = hosts.mapNotNull { state.objectRef(it) }.toSet()
        val rebound = state.updateEntity(auraId) {
            it.with(GainedEnchantRestrictionComponent(filter = effect.filter, hosts = refs))
        }

        val host = hosts.firstOrNull {
            AttachmentMover.canAttach(rebound, predicateEvaluator, cardRegistry, auraId, it)
        } ?: return EffectResult.success(rebound)
        val (attached, events) = AttachmentMover.attach(rebound, auraId, host, context.controllerId)
        return EffectResult.success(attached, events)
    }
}
