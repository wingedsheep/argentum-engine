package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.standingStatics
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.RestrictCardsDrawnPerTurn
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reader for [RestrictCardsDrawnPerTurn] — "each opponent can't draw more than one card each turn"
 * (CR 121.2b). Consulted by [DrawLoop] before every individual draw (and before any draw
 * replacement, since a draw the player can't make can't be replaced) and by [DrawUpToExecutor] to
 * cap the offered number.
 */
object DrawRestrictions {

    /**
     * How many more cards [playerId] may draw this turn, or `null` when nothing caps them.
     *
     * The cap usually sits on an opponent's permanent, so every standing static is read (see
     * [standingStatics]); `affected` is resolved from each source's controller. The smallest
     * applicable cap wins, and the count already drawn this turn includes cards drawn before the
     * cap's source entered.
     */
    fun drawsLeftThisTurn(
        state: GameState,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        predicates: PredicateEvaluator
    ): Int? {
        val cap = standingStatics(state, cardRegistry, predicates, RestrictCardsDrawnPerTurn::class) { ability, source, controller ->
            playerId in TargetResolutionUtils.resolvePlayerTargets(
                EffectTarget.PlayerRef(ability.affected), state,
                EffectContext(sourceId = source, controllerId = controller)
            )
        }.minOfOrNull { it.maxPerTurn } ?: return null
        val drawn = state.getEntity(playerId)?.get<CardsDrawnThisTurnComponent>()?.count ?: 0
        return (cap - drawn).coerceAtLeast(0)
    }

    /** True when a [RestrictCardsDrawnPerTurn] cap forbids [playerId]'s next draw this turn. */
    fun cantDraw(
        state: GameState,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        predicates: PredicateEvaluator
    ): Boolean = drawsLeftThisTurn(state, playerId, cardRegistry, predicates) == 0
}
