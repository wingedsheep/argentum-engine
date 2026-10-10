package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyKeywordActionAmount
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Applies [ModifyKeywordActionAmount] replacement effects to an announced keyword-action count —
 * mill (CR 701.17), scry (CR 701.22) or surveil (CR 701.25) — before any card moves. Called once at
 * each action's announcement site ([GatherCardsExecutor]'s `CardSource.TopOfLibrary` branch for the
 * flagged gather, and the mill cost atom), NOT per moved card, so a paused-and-resumed pipeline
 * never double-modifies.
 *
 * The keyword-action twin of `DrawReplacementDispatcher.applyDrawAmountModifier`: it scans every
 * battlefield permanent's replacement effects for the matching action's pattern, gates each by that
 * pattern's `player` filter relative to the acting player and the source's projected controller, checks
 * `restrictions`, sums the modifier, and clamps the result to `≥ 0`. A base count of 0 is left
 * untouched — a mill, scry or surveil of 0 is no event.
 */
object KeywordActionAmountModifier {

    fun mill(state: GameState, playerId: EntityId, originalCount: Int, predicateEvaluator: PredicateEvaluator): Int =
        applyAmountModifiers(state, playerId, originalCount, predicateEvaluator) {
            (it as? EventPattern.MillEvent)?.player
        }

    fun scry(state: GameState, playerId: EntityId, originalCount: Int, predicateEvaluator: PredicateEvaluator): Int =
        applyAmountModifiers(state, playerId, originalCount, predicateEvaluator) {
            (it as? EventPattern.ScryEvent)?.player
        }

    fun surveil(state: GameState, playerId: EntityId, originalCount: Int, predicateEvaluator: PredicateEvaluator): Int =
        applyAmountModifiers(state, playerId, originalCount, predicateEvaluator) {
            (it as? EventPattern.SurveilEvent)?.player
        }
}

/**
 * Sum every battlefield permanent's [ModifyKeywordActionAmount] whose `appliesTo` is the wanted
 * action — [playerOf] returns that pattern's `player`, or null for another action — onto
 * [originalCount], gated by that `player` filter relative to [playerId] and the source's controller
 * and by its `restrictions`, clamped to `≥ 0`. A count of 0 is no event and is never modified.
 */
private fun applyAmountModifiers(
    state: GameState,
    playerId: EntityId,
    originalCount: Int,
    predicateEvaluator: PredicateEvaluator,
    playerOf: (EventPattern) -> Player?
): Int {
    if (originalCount <= 0) return originalCount
    val conditionEvaluator = predicateEvaluator.conditions
    var adjusted = originalCount
    for (entityId in state.getBattlefield()) {
        val replacementSource = state.getEntity(entityId)?.get<ReplacementEffectSourceComponent>() ?: continue
        val sourceControllerId = state.projectedState.getController(entityId)

        for (effect in replacementSource.replacementEffects) {
            if (effect !is ModifyKeywordActionAmount) continue
            val player = playerOf(effect.appliesTo) ?: continue

            val matchesPlayer = when (player) {
                Player.Each -> true
                Player.You -> sourceControllerId != null && playerId == sourceControllerId
                Player.EachOpponent ->
                    sourceControllerId != null && state.isOpponentOf(playerId, sourceControllerId)
                else -> false
            }
            if (!matchesPlayer) continue

            val effectContext = EffectContext(
                sourceId = entityId,
                controllerId = playerId,
            )
            val restrictionsHold = effect.restrictions.all { restriction ->
                conditionEvaluator.evaluate(state, restriction, effectContext)
            }
            if (!restrictionsHold) continue

            adjusted += effect.modifier
        }
    }
    return adjusted.coerceAtLeast(0)
}
