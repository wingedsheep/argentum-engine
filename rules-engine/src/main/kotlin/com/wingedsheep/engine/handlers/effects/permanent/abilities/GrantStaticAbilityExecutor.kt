package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.GrantStaticAbilityEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantStaticAbilityEffect].
 * "Target creature gains '[static ability]' until end of turn"
 *
 * Adds the static ability to [GameState.grantedStaticAbilities], where the relevant
 * point-of-use checks (e.g. combat blocker validation for
 * [com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan]) consult it alongside the
 * creature's printed static abilities. Mirrors [GrantTriggeredAbilityExecutor].
 */
class GrantStaticAbilityExecutor : EffectExecutor<GrantStaticAbilityEffect> {

    override val effectType: KClass<GrantStaticAbilityEffect> =
        GrantStaticAbilityEffect::class

    override fun execute(
        state: GameState,
        effect: GrantStaticAbilityEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for static ability grant")

        state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target no longer exists")

        // A *player* may hold a grant. Some static abilities describe a rule about the game rather
        // than about an object — High Tide's "until end of turn, whenever a player taps an Island
        // for mana, that player adds an additional {U}" — and a spell has no permanent to anchor
        // one to. The holder then only supplies the "you" of any controller predicate in the
        // static's own filters; it is not the thing the static acts on.
        if (state.turnOrder.contains(targetId)) {
            return EffectResult.success(
                state.copy(
                    grantedStaticAbilities = state.grantedStaticAbilities + GrantedStaticAbility(
                        entityId = targetId,
                        ability = effect.ability,
                        duration = effect.duration,
                        sourceId = context.sourceId
                    )
                )
            )
        }

        state.getEntity(targetId)?.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")
        // Battlefield permanents are the common case, but a static ability can also be handed to a
        // *card* — "creature cards in your graveyard gain 'You may cast this card from your
        // graveyard' until end of turn" (Case of the Uneaten Feast). The graveyard-cast read sites
        // treat a grant anchored to a graveyard card as that card's own permission, so the grant
        // has to be allowed to land there. Anywhere else is still rejected.
        val onBattlefield = state.getBattlefield().contains(targetId)
        val inAGraveyard = state.zones.any { (key, ids) ->
            key.zoneType == Zone.GRAVEYARD && targetId in ids
        }
        if (!onBattlefield && !inAGraveyard) {
            return EffectResult.error(state, "Target is not on the battlefield or in a graveyard")
        }

        val grant = GrantedStaticAbility(
            entityId = targetId,
            ability = effect.ability,
            duration = effect.duration,
            sourceId = context.sourceId
        )

        val newState = state.copy(
            grantedStaticAbilities = state.grantedStaticAbilities + grant
        )

        return EffectResult.success(newState)
    }
}
