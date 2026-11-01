package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.serialization.ScriptTreeWalker

/**
 * Freezes the parts of a filter that only the resolving context can answer — the player
 * references and, when an evaluator is supplied, the dynamic numbers — for effects whose filter
 * outlives the resolution that created it.
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
 *
 * With [DynamicAmountEvaluator] supplied, the dynamic-amount card predicates are evaluated once
 * against the resolving context and rewritten to their fixed siblings —
 * [CardPredicate.PowerAtMostDynamic] → [CardPredicate.PowerAtMost], and likewise for
 * `PowerEqualsDynamic`, `ToughnessEqualsDynamic`, `ManaValueAtMostDynamic` and
 * `ManaValueEqualsDynamic`. Information an effect needs from the game is determined only once,
 * when the effect is applied (CR 608.2h): Rumbling Ruin's "creatures your opponents control with power less than or equal to
 * that number can't block this turn" counts the +1/+1 counters as the ability resolves, while
 * which creatures have power that low stays live. Only the number is baked, never the matching
 * objects. The layer projector can't evaluate a dynamic amount (it has no resolving context), so
 * an unbaked dynamic predicate in a floating group filter matches nothing. Amounts phrased
 * relative to the object under test (`Player.Candidate`) have no single value and stay symbolic.
 */
object ResolutionFilterBaker {

    fun bake(
        filter: GroupFilter,
        context: EffectContext,
        state: GameState,
        amounts: DynamicAmountEvaluator? = null
    ): GroupFilter {
        val baked = bake(filter.baseFilter, context, state, amounts)
        return if (baked == filter.baseFilter) filter else filter.copy(baseFilter = baked)
    }

    fun bake(
        filter: GameObjectFilter,
        context: EffectContext,
        state: GameState,
        amounts: DynamicAmountEvaluator? = null
    ): GameObjectFilter {
        val predicate = filter.controllerPredicate?.let { bake(it, context, state) }
        val cardPredicates =
            if (amounts == null) filter.cardPredicates
            else filter.cardPredicates.map { bake(it, context, state, amounts) }
        val anyOf = filter.anyOf.map { bake(it, context, state, amounts) }
        return if (predicate == filter.controllerPredicate && cardPredicates == filter.cardPredicates &&
            anyOf == filter.anyOf
        ) filter
        else filter.copy(controllerPredicate = predicate, cardPredicates = cardPredicates, anyOf = anyOf)
    }

    private fun bake(
        predicate: CardPredicate,
        context: EffectContext,
        state: GameState,
        amounts: DynamicAmountEvaluator
    ): CardPredicate {
        fun value(amount: DynamicAmount): Int? =
            if (referencesCandidate(amount)) null else amounts.evaluate(state, amount, context)
        return when (predicate) {
            is CardPredicate.PowerAtMostDynamic -> value(predicate.amount)?.let { CardPredicate.PowerAtMost(it) }
            is CardPredicate.PowerEqualsDynamic -> value(predicate.amount)?.let { CardPredicate.PowerEquals(it) }
            is CardPredicate.ToughnessEqualsDynamic ->
                value(predicate.amount)?.let { CardPredicate.ToughnessEquals(it) }
            is CardPredicate.ManaValueAtMostDynamic ->
                value(predicate.amount)?.let { CardPredicate.ManaValueAtMost(it) }
            is CardPredicate.ManaValueEqualsDynamic ->
                value(predicate.amount)?.let { CardPredicate.ManaValueEquals(it) }
            is CardPredicate.And -> CardPredicate.And(predicate.predicates.map { bake(it, context, state, amounts) })
            is CardPredicate.Or -> CardPredicate.Or(predicate.predicates.map { bake(it, context, state, amounts) })
            is CardPredicate.Not -> CardPredicate.Not(bake(predicate.predicate, context, state, amounts))
            else -> null
        } ?: predicate
    }

    /** Whether [amount] reads the object under test, which has no value at resolution. */
    private fun referencesCandidate(amount: DynamicAmount): Boolean {
        var found = false
        ScriptTreeWalker.forEachNode(DynamicAmount.serializer(), amount) { if (it == Player.Candidate) found = true }
        return found
    }

    /**
     * [target] pinned to the concrete player it names now, when it is context-bound (a target, a
     * chosen or triggering player) — Xanathar, Guild Kingpin's "their library" on a granted
     * library-top permission. Left as is when it is state-relative or names no player here.
     */
    fun bakePlayer(target: EffectTarget, context: EffectContext, state: GameState): EffectTarget =
        if (!isContextBound(target)) target
        else context.resolvePlayerTarget(target, state)
            ?.takeIf { it in state.turnOrder }
            ?.let { EffectTarget.SpecificEntity(it) }
            ?: target

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
