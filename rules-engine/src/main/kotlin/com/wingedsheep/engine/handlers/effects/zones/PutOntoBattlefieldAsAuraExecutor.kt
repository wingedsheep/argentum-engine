package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.EffectCopyAuraEntryContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.effects.copy.CopyAuraEntry
import com.wingedsheep.engine.handlers.predicates.EnchantRestriction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.PutOntoBattlefieldAsAuraEffect
import kotlin.reflect.KClass

/**
 * Executor for [PutOntoBattlefieldAsAuraEffect] — "return it to the battlefield. It's an Aura
 * enchantment with enchant Forest you control and \"…\", and it loses all other abilities" (Harold
 * and Bob, First Numens).
 *
 * The card enters under the effect controller's control (the Harold and Bob ruling: "It doesn't
 * matter who owns the card"), already the Aura [AuraEntry] describes. Its controller chooses what
 * it enchants as it enters (CR 303.4f) among the permanents its new enchant ability admits that
 * would hold it — read against the card *as that Aura*, so protection from creatures doesn't turn
 * it away and protection from enchantments does. The choice pauses on the shared Aura-entry
 * question and replays this effect with the answer, like every other effect-driven Aura entry
 * ([CopyAuraEntry]). With nothing to enchant the card doesn't move (CR 303.4g).
 */
class PutOntoBattlefieldAsAuraExecutor(
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry,
) : EffectExecutor<PutOntoBattlefieldAsAuraEffect> {

    override val effectType: KClass<PutOntoBattlefieldAsAuraEffect> = PutOntoBattlefieldAsAuraEffect::class

    override fun execute(
        state: GameState,
        effect: PutOntoBattlefieldAsAuraEffect,
        context: EffectContext
    ): EffectResult {
        val cardId = context.resolveTarget(effect.target, state) ?: return EffectResult.success(state)
        val card = state.getEntity(cardId)?.get<CardComponent>() ?: return EffectResult.success(state)
        val zone = state.zones.keys.firstOrNull { cardId in state.getZone(it) }
            ?: return EffectResult.success(state)
        if (zone.zoneType == Zone.BATTLEFIELD || zone.zoneType == Zone.STACK) return EffectResult.success(state)
        if (effect.fromZone != null && zone.zoneType != effect.fromZone) return EffectResult.success(state)
        if (EntryLocks.cantEnter(state, cardId, zone.zoneType, cardRegistry, zones.predicateEvaluator)) {
            return EffectResult.success(state)
        }

        val controllerId = context.controllerId
        val entry = AuraEntry(effect.enchant, effect.grantedStaticAbilities, effect.grantedAbilities)
        val host = if (cardId in context.entryAuraHosts) {
            context.entryAuraHosts[cardId]
        } else {
            val hosts = legalHosts(state, cardId, entry.preview(card), controllerId, entry)
            if (hosts.isEmpty()) return EffectResult.success(state)
            return EffectResult.from(state.suspendForDecision(
                CopyAuraEntry.question(cardId, card.name, controllerId, hosts),
                EffectCopyAuraEntryContinuation(effect, context, cardId)
            ))
        } ?: return EffectResult.success(state)

        val moved = zones.moveToZone(
            state, cardId, Zone.BATTLEFIELD,
            ZoneEntryOptions(controllerId = controllerId, auraHostId = host, asAura = entry), zone
        )
        return EffectResult.success(moved.state, moved.events)
    }

    /**
     * The permanents [aura] could enchant once it is the Aura [entry] makes it: matching the new
     * enchant ability with "you" as [controllerId], allowing the Aura ("can't be enchanted"), and
     * not protected from its characteristics as that Aura.
     */
    private fun legalHosts(
        state: GameState, cardId: EntityId, aura: CardComponent, controllerId: EntityId, entry: AuraEntry,
    ): List<EntityId> {
        val preview = state.updateEntity(cardId) { it.with(aura).with(ControllerComponent(controllerId)) }
        val projected = preview.projectedState
        val evaluator = zones.predicateEvaluator
        val context = PredicateContext(controllerId = controllerId, sourceId = cardId)
        return preview.getBattlefield().filter { host ->
            host != cardId &&
                evaluator.matches(preview, projected, host, entry.enchant, context) &&
                EnchantRestriction.hostAllowsAura(preview, projected, evaluator, cardId, host) &&
                !EnchantRestriction.hostProtectedFromAttachment(preview, projected, cardRegistry, cardId, aura, host)
        }
    }
}
