package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.mechanics.durations.GrantDurationGate
import com.wingedsheep.engine.state.components.battlefield.SuppressesHexproofForGroupComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.SuppressHexproofForGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Shared check for "creatures matching filter F can be targeted as though they didn't have
 * hexproof" effects (e.g. Nowhere to Run, or Detection Tower's until-end-of-turn grant), and its player half — "your opponents … can be the
 * targets … as though they didn't have hexproof" (Kaya, Bane of the Dead), read by
 * [ControllerHexproof.appliesAgainst]. Consulted at three points:
 *  - cast-time legal-action enumeration ([com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils])
 *  - cast-time target validation ([TargetValidator])
 *  - resolution-time target legality ([com.wingedsheep.engine.mechanics.stack.StackResolver])
 *
 * Hexproof bypass is per-caster: only suppressors controlled by [casterId] grant the bypass.
 */
object HexproofSuppression {

    /**
     * One live "as though they didn't have hexproof" grant: whose spells and abilities it frees
     * ([youId]), the object it is read from ([holderId], the predicate context's source), and what
     * it reaches.
     */
    private class Suppressor(
        val holderId: EntityId,
        val youId: EntityId,
        val filters: List<GroupFilter>,
        val players: List<Player>
    )

    /**
     * Every live suppressor controlled by [casterId]: the printed [SuppressHexproofForGroup] statics
     * baked onto battlefield permanents, plus the ones a resolving effect handed out through
     * [GameState.grantedStaticAbilities] — Detection Tower's "until end of turn, your opponents and
     * creatures your opponents control with hexproof can be the targets …", which is granted to
     * the activating *player* so it outlives the land (CR 611.2a). A player-held grant frees its
     * holder's spells; a permanent-held one frees the permanent's controller's. Lazy, so a hit on
     * the first suppressor skips the rest.
     */
    private fun suppressorsFor(state: GameState, projected: ProjectedState, casterId: EntityId): Sequence<Suppressor> {
        val battlefield = state.getBattlefield()
        val printed = battlefield.asSequence().mapNotNull { suppressorId ->
            if (projected.getController(suppressorId) != casterId) return@mapNotNull null
            val component = state.getEntity(suppressorId)
                ?.get<SuppressesHexproofForGroupComponent>() ?: return@mapNotNull null
            Suppressor(suppressorId, casterId, component.filters, component.players)
        }
        if (state.grantedStaticAbilities.isEmpty()) return printed
        val granted = state.grantedStaticAbilities.asSequence().mapNotNull { grant ->
            val ability = grant.ability as? SuppressHexproofForGroup ?: return@mapNotNull null
            val holder = grant.entityId
            val isPlayer = holder in state.turnOrder
            if (!isPlayer && (holder !in battlefield || projected.hasLostAllAbilities(holder))) return@mapNotNull null
            val you = if (isPlayer) holder else projected.getController(holder)
            if (you != casterId) return@mapNotNull null
            if (!GrantDurationGate.holds(state, holder, grant.sourceId, grant.duration)) return@mapNotNull null
            Suppressor(holder, you, listOf(ability.filter), listOfNotNull(ability.players))
        }
        return printed + granted
    }

    fun isSuppressedForCaster(
        state: GameState,
        projected: ProjectedState,
        targetId: EntityId,
        casterId: EntityId,
        predicateEvaluator: PredicateEvaluator
    ): Boolean = suppressorsFor(state, projected, casterId).any { suppressor ->
        val ctx = PredicateContext(controllerId = suppressor.youId, sourceId = suppressor.holderId)
        suppressor.filters.any { groupFilter ->
            when {
                groupFilter.scope !is Scope.Battlefield -> false
                groupFilter.excludeSelf && targetId == suppressor.holderId -> false
                else -> predicateEvaluator.matches(
                    state, projected, targetId, groupFilter.baseFilter, ctx
                )
            }
        }
    }

    /**
     * Whether a suppressor controlled by [casterId] lets [casterId]'s spells and abilities target
     * [playerId] as though that player didn't have hexproof ([SuppressHexproofForGroup.players]).
     */
    fun isPlayerSuppressedForCaster(
        state: GameState,
        playerId: EntityId,
        casterId: EntityId,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        val projected = state.projectedState
        return suppressorsFor(state, projected, casterId).any { suppressor ->
            val ctx = PredicateContext(controllerId = suppressor.youId, sourceId = suppressor.holderId)
            suppressor.players.any { predicateEvaluator.matchesPlayer(state, projected, it, playerId, ctx) }
        }
    }
}
