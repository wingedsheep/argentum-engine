package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GrantedKeywordAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.GrantHarmonizeEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantHarmonizeEffect] — Songcrafter Mage: "target instant or sorcery card in
 * your graveyard gains harmonize until end of turn. Its harmonize cost is equal to its mana
 * cost." (CR 702.180.)
 *
 * Resolves the target card, derives the harmonize cost (the effect's fixed cost if given, else
 * the card's own mana cost), and records a [GrantedKeywordAbility] keyed to the card entity.
 * From then on the harmonize read sites — the cast-from-graveyard enumerator, the cast handler,
 * the alternative-payment handler, and the stack resolver — see the grant via [com.wingedsheep
 * .engine.mechanics.HarmonizeGrants], so the card can be cast from the graveyard exactly like a
 * printed-harmonize card (tap-for-power reduction, exile on resolution). The grant is removed at
 * end of turn by the cleanup step.
 */
class GrantHarmonizeExecutor : EffectExecutor<GrantHarmonizeEffect> {

    override val effectType: KClass<GrantHarmonizeEffect> = GrantHarmonizeEffect::class

    override fun execute(
        state: GameState,
        effect: GrantHarmonizeEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for harmonize grant")

        val container = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target card no longer exists")
        val cardComponent = container.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")

        // The target must be an instant or sorcery card in a graveyard (the printed type line is
        // authoritative for a graveyard card — no projection needed off the battlefield).
        if (!cardComponent.typeLine.isInstant && !cardComponent.typeLine.isSorcery) {
            return EffectResult.error(state, "Harmonize can only be granted to an instant or sorcery")
        }
        val inAGraveyard = state.zones.any { (key, ids) ->
            key.zoneType == Zone.GRAVEYARD && targetId in ids
        }
        if (!inAGraveyard) {
            return EffectResult.error(state, "Target is not in a graveyard")
        }

        // "Its harmonize cost is equal to its mana cost" — unless the effect fixes a cost.
        val harmonizeCost = effect.cost ?: cardComponent.manaCost

        val grant = GrantedKeywordAbility(
            entityId = targetId,
            ability = KeywordAbility.Harmonize(harmonizeCost),
            duration = effect.duration
        )

        val newState = state.copy(
            grantedKeywordAbilities = state.grantedKeywordAbilities + grant
        )

        return EffectResult.success(newState)
    }
}
