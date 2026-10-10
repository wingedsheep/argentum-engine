package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.StaticAbilityGrantedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.effects.GrantStaticAbilityEffect
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
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

        val ability = bakeResolutionPlayers(effect.ability, context, state)

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
                        ability = ability,
                        duration = effect.duration,
                        sourceId = context.sourceId,
                        controllerId = context.controllerId
                    )
                ),
                listOf(StaticAbilityGrantedEvent(targetId))
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
            ability = ability,
            duration = effect.duration,
            sourceId = context.sourceId,
            controllerId = context.controllerId,
            // A permanent that gains a static ability has it like a printed one: its layer-system
            // half is projected (see GrantedStaticAbility.layerTimestamp). Point-of-use readers
            // keep consulting the record for the halves that never lower to a continuous effect.
            layerTimestamp = if (onBattlefield) state.timestamp else null
        )

        val newState = state.copy(
            grantedStaticAbilities = state.grantedStaticAbilities + grant
        )

        return EffectResult.success(newState, listOf(StaticAbilityGrantedEvent(targetId)))
    }

    /**
     * Freeze player references that only the resolving context can answer into concrete ids.
     *
     * A grant outlives the resolution that created it: its filters are read later, at the point
     * of use (block declaration), where the pipeline's stored choices and the ability's targets
     * are gone. The Black Gate's "choose a player with the most life … target creature can't be
     * blocked by creatures that player controls this turn" names a player chosen mid-resolution;
     * left symbolic, `ControlledByReferencedPlayer(PipelineTarget)` would resolve to nobody and
     * the creature could be blocked by everyone. Per the card's ruling, the player is fixed when
     * the ability resolves while the set of their creatures stays live — so only the player is
     * baked, never the blockers.
     *
     * Only context-bound references ([EffectTarget.PipelineTarget], [EffectTarget.ContextTarget],
     * [EffectTarget.BoundVariable]) are rewritten; state-relative ones (defending player, you)
     * keep resolving at the point of use as before. Extend the `when` below when another granted
     * static carries a player-scoped filter.
     */
    private fun bakeResolutionPlayers(
        ability: StaticAbility,
        context: EffectContext,
        state: GameState
    ): StaticAbility = when (ability) {
        is CantBeBlockedBy -> {
            val baked = bakeFilter(ability.blockerFilter, context, state)
            if (baked == ability.blockerFilter) ability else ability.copy(blockerFilter = baked)
        }
        else -> ability
    }

    private fun bakeFilter(filter: GameObjectFilter, context: EffectContext, state: GameState): GameObjectFilter =
        filter.copy(
            controllerPredicate = filter.controllerPredicate?.let { bakePredicate(it, context, state) },
            anyOf = filter.anyOf.map { bakeFilter(it, context, state) }
        )

    private fun bakePredicate(
        predicate: ControllerPredicate,
        context: EffectContext,
        state: GameState
    ): ControllerPredicate = when (predicate) {
        is ControllerPredicate.ControlledByReferencedPlayer -> when (predicate.target) {
            is EffectTarget.PipelineTarget, is EffectTarget.ContextTarget, is EffectTarget.BoundVariable -> {
                val playerId = context.resolvePlayerTarget(predicate.target, state)
                    ?.takeIf { it in state.turnOrder }
                if (playerId != null) {
                    ControllerPredicate.ControlledByReferencedPlayer(EffectTarget.SpecificEntity(playerId))
                } else predicate
            }
            else -> predicate
        }
        is ControllerPredicate.And -> ControllerPredicate.And(predicate.predicates.map { bakePredicate(it, context, state) })
        is ControllerPredicate.Or -> ControllerPredicate.Or(predicate.predicates.map { bakePredicate(it, context, state) })
        is ControllerPredicate.Not -> ControllerPredicate.Not(bakePredicate(predicate.predicate, context, state))
        else -> predicate
    }
}
