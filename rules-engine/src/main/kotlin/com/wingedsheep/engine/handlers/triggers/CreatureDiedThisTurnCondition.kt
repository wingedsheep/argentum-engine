package com.wingedsheep.engine.handlers.triggers

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.CreaturesDiedThisTurnComponent
import com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition as CreatureDiedThisTurnSdkCondition
import kotlin.reflect.KClass

/**
 * Evaluator for CreatureDiedThisTurnCondition.
 * Intervening-if: "if a creature died this turn" — global, any creature, any controller.
 * ZoneTransitionService increments CreaturesDiedThisTurnComponent on the dying creature's
 * controller, so this sums across every player. CleanupPhaseManager clears at end of turn.
 */
class CreatureDiedThisTurnConditionEvaluator {

    val conditionType: KClass<CreatureDiedThisTurnSdkCondition> = CreatureDiedThisTurnSdkCondition::class

    fun evaluate(
        state: GameState,
        @Suppress("UNUSED_PARAMETER") condition: CreatureDiedThisTurnSdkCondition,
        @Suppress("UNUSED_PARAMETER") context: EffectContext
    ): Boolean {
        return state.turnOrder.any { playerId ->
            (state.getEntity(playerId)?.get<CreaturesDiedThisTurnComponent>()?.count ?: 0) > 0
        }
    }
}
