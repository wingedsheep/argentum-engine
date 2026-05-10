package com.wingedsheep.engine.handlers.permissions

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Returns true when the optional condition gate on a [MayPlayFromExileComponent] is open
 * (or absent). Conditional grants — e.g. Possibility Technician's "you may play it if you
 * control a Kavu" — must re-evaluate the condition at every read site (legal-action
 * enumeration, cast validation, land-play validation, client view), not just at the moment
 * the stamp was created.
 */
fun MayPlayFromExileComponent.gateOpen(
    state: GameState,
    cardId: EntityId,
    conditionEvaluator: ConditionEvaluator,
): Boolean {
    val condition = condition ?: return true
    val opponentId = state.turnOrder.firstOrNull { it != controllerId }
    val context = EffectContext(
        sourceId = cardId,
        controllerId = controllerId,
        opponentId = opponentId,
    )
    return conditionEvaluator.evaluate(state, condition, context)
}
