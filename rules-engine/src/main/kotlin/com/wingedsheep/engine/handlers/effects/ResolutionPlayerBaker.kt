package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Freezes the player references in a filter that only the resolving context can answer into
 * concrete player ids, for effects whose filter outlives the resolution that created it.
 *
 * A granted static ability or a "can't block this turn" group effect is read later — at block
 * declaration, on every projection — where the ability's targets, its pipeline choices, its
 * trigger and its combat position are gone. The Black Gate's "creatures that player controls"
 * names a player chosen mid-resolution; The Motherlode, Excavator's "creatures that player
 * controls without flying can't block this turn" names the defending player, which can no longer
 * be read off the source once it has left combat. Per those cards' rulings the player is fixed
 * when the ability resolves while the set of their creatures stays live — so only the player is
 * baked, never the matching objects.
 *
 * Context-bound references are rewritten to [EffectTarget.SpecificEntity]: positional, pipeline
 * and bound-variable targets, the target / triggering / chosen / defending player, and the
 * leaf predicates [ControllerPredicate.ControlledByTargetPlayer],
 * [ControllerPredicate.ControlledByTargetOpponent] and
 * [ControllerPredicate.ControlledByTriggeringPlayer]. Anything that doesn't resolve now (no
 * such player in this context) is left symbolic, as are the state-relative references (you,
 * the active player, any defending player) that mean the same thing at the point of use.
 */
object ResolutionPlayerBaker {

    fun bake(filter: GroupFilter, context: EffectContext, state: GameState): GroupFilter {
        val baked = bake(filter.baseFilter, context, state)
        return if (baked == filter.baseFilter) filter else filter.copy(baseFilter = baked)
    }

    fun bake(filter: GameObjectFilter, context: EffectContext, state: GameState): GameObjectFilter {
        val predicate = filter.controllerPredicate?.let { bake(it, context, state) }
        val anyOf = filter.anyOf.map { bake(it, context, state) }
        return if (predicate == filter.controllerPredicate && anyOf == filter.anyOf) filter
        else filter.copy(controllerPredicate = predicate, anyOf = anyOf)
    }

    private fun bake(
        predicate: ControllerPredicate,
        context: EffectContext,
        state: GameState
    ): ControllerPredicate = when (predicate) {
        is ControllerPredicate.ControlledByReferencedPlayer ->
            if (isContextBound(predicate.target)) pinned(predicate.target, context, state) ?: predicate
            else predicate
        ControllerPredicate.ControlledByTargetPlayer ->
            pinned(EffectTarget.PlayerRef(Player.TargetPlayer), context, state) ?: predicate
        ControllerPredicate.ControlledByTargetOpponent ->
            pinned(EffectTarget.PlayerRef(Player.TargetOpponent), context, state) ?: predicate
        ControllerPredicate.ControlledByTriggeringPlayer ->
            pinned(EffectTarget.PlayerRef(Player.TriggeringPlayer), context, state) ?: predicate
        is ControllerPredicate.And -> ControllerPredicate.And(predicate.predicates.map { bake(it, context, state) })
        is ControllerPredicate.Or -> ControllerPredicate.Or(predicate.predicates.map { bake(it, context, state) })
        is ControllerPredicate.Not -> ControllerPredicate.Not(bake(predicate.predicate, context, state))
        else -> predicate
    }

    private fun pinned(target: EffectTarget, context: EffectContext, state: GameState): ControllerPredicate? =
        context.resolvePlayerTarget(target, state)
            ?.takeIf { it in state.turnOrder }
            ?.let { ControllerPredicate.ControlledByReferencedPlayer(EffectTarget.SpecificEntity(it)) }

    private fun isContextBound(target: EffectTarget): Boolean = when (target) {
        is EffectTarget.PipelineTarget, is EffectTarget.ContextTarget, is EffectTarget.BoundVariable -> true
        is EffectTarget.PlayerRef -> when (target.player) {
            Player.TargetPlayer, Player.TargetOpponent, Player.Any, Player.TriggeringPlayer,
            Player.DefendingPlayer, Player.ChosenOpponent, Player.ChosenPlayer,
            is Player.ContextPlayer, is Player.BoundVariable -> true
            else -> false
        }
        else -> false
    }
}
