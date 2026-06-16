package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GrantedKeywordAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.GrantFlashbackEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantFlashbackEffect] — Archmage's Newt: "target instant or sorcery card in your
 * graveyard gains flashback until end of turn. The flashback cost is equal to its mana cost. That
 * card gains flashback {0} until end of turn instead if this creature is saddled." (CR 702.34.)
 *
 * Resolves the target card, derives the flashback cost (the effect's fixed cost if given — e.g.
 * `{0}` on the saddled branch — else the card's own mana cost), and records a
 * [GrantedKeywordAbility] keyed to the card entity. From then on the flashback read sites — the
 * cast-from-graveyard enumerator, the cast handler / zone resolver, and the stack resolver — see
 * the grant via [com.wingedsheep.engine.mechanics.FlashbackGrants], so the card can be cast from
 * the graveyard exactly like a printed-flashback card (exile on resolution). The grant is removed
 * at end of turn by the cleanup step. Mirrors [GrantHarmonizeExecutor].
 */
class GrantFlashbackExecutor : EffectExecutor<GrantFlashbackEffect> {

    override val effectType: KClass<GrantFlashbackEffect> = GrantFlashbackEffect::class

    override fun execute(
        state: GameState,
        effect: GrantFlashbackEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for flashback grant")

        val container = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target card no longer exists")
        val cardComponent = container.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")

        // The target must be an instant or sorcery card in a graveyard (the printed type line is
        // authoritative for a graveyard card — no projection needed off the battlefield).
        if (!cardComponent.typeLine.isInstant && !cardComponent.typeLine.isSorcery) {
            return EffectResult.error(state, "Flashback can only be granted to an instant or sorcery")
        }
        val inAGraveyard = state.zones.any { (key, ids) ->
            key.zoneType == Zone.GRAVEYARD && targetId in ids
        }
        if (!inAGraveyard) {
            return EffectResult.error(state, "Target is not in a graveyard")
        }

        // "The flashback cost is equal to its mana cost" — unless the effect fixes a cost ({0} when saddled).
        val flashbackCost = effect.cost ?: cardComponent.manaCost

        val grant = GrantedKeywordAbility(
            entityId = targetId,
            ability = KeywordAbility.Flashback(flashbackCost),
            duration = effect.duration
        )

        val newState = state.copy(
            grantedKeywordAbilities = state.grantedKeywordAbilities + grant
        )

        return EffectResult.success(newState)
    }
}
