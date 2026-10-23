package com.wingedsheep.engine.mechanics.dungeon

import com.wingedsheep.engine.core.standingStatics
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.VenturesThisTurnComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.RestrictVenturesPerTurn
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reader for [RestrictVenturesPerTurn] — "each opponent can't venture into the dungeon more than
 * once each turn". Consulted by
 * [com.wingedsheep.engine.handlers.effects.player.VentureIntoTheDungeonExecutor] before every
 * venture, the initiative's included.
 */
object VentureRestrictions {

    /**
     * True when a [RestrictVenturesPerTurn] cap forbids [playerId]'s next venture this turn.
     *
     * The cap usually sits on an opponent's permanent, so every standing static is read;
     * `affected` is resolved from each source's controller. The smallest applicable cap wins, and
     * ventures made before the cap's source entered count.
     */
    fun cantVenture(
        state: GameState,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        predicates: PredicateEvaluator
    ): Boolean {
        val cap = standingStatics(state, cardRegistry, predicates, RestrictVenturesPerTurn::class) { ability, source, controller ->
            playerId in TargetResolutionUtils.resolvePlayerTargets(
                EffectTarget.PlayerRef(ability.affected), state,
                EffectContext(sourceId = source, controllerId = controller)
            )
        }.minOfOrNull { it.maxPerTurn } ?: return false
        val ventured = state.getEntity(playerId)?.get<VenturesThisTurnComponent>()?.count ?: 0
        return ventured >= cap
    }
}
