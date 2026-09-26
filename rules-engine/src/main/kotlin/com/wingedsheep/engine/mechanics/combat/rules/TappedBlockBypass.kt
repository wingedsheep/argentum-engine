package com.wingedsheep.engine.mechanics.combat.rules

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.SoulbondPairing
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CanBlockAsThoughUntapped
import com.wingedsheep.sdk.scripting.filters.unified.Scope

/**
 * Single source of truth for "is this creature's tapped status stopping it from blocking?".
 *
 * CR 509.1a lets the defending player choose *untapped* creatures as blockers. A
 * [CanBlockAsThoughUntapped] static lifts that one requirement:
 *  - self scope, printed on the creature itself; attached scope on an Aura/Equipment, or
 *  - battlefield scope on any permanent whose group filter matches the creature (Masako the
 *    Humorless: "Tapped creatures you control can block as though they were untapped"), matched
 *    with that permanent as predicate source and its controller as "you" against the current
 *    projection. A face-down permanent has no abilities (CR 708.2) and contributes nothing.
 *
 * Every other blocking restriction still applies — callers keep running their remaining checks.
 * Consulted by every blocker-eligibility path (`TurnManager.getValidBlockers`, `BlockPhaseManager`'s
 * validation and requirement checks) so the offered blockers and the authoritative validation agree.
 */
object TappedBlockBypass {

    /** True when [entityId] is tapped and no [CanBlockAsThoughUntapped] covers it. */
    fun tappedPreventsBlocking(
        state: GameState,
        entityId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        val container = state.getEntity(entityId) ?: return true
        if (!container.has<TappedComponent>()) return false
        return !isActive(state, entityId, cardRegistry, predicateEvaluator)
    }

    fun isActive(
        state: GameState,
        entityId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        val projected = state.projectedState
        for (permanentId in state.getBattlefield()) {
            val permanent = state.getEntity(permanentId) ?: continue
            if (permanent.has<FaceDownComponent>()) continue
            val permCard = permanent.get<CardComponent>() ?: continue
            val abilities = cardRegistry.getCard(permCard.cardDefinitionId)?.staticAbilities ?: continue
            val grants = abilities.filterIsInstance<CanBlockAsThoughUntapped>()
            if (grants.isEmpty()) continue
            val permController = projected.getController(permanentId) ?: continue
            val predicateContext = PredicateContext(controllerId = permController, sourceId = permanentId)
            for (grant in grants) {
                val covers = when (val scope = grant.filter.scope) {
                    is Scope.Self -> permanentId == entityId
                    is Scope.Specific -> scope.entityId == entityId
                    is Scope.AttachedTo -> permanent.get<AttachedToComponent>()?.targetId == entityId
                    is Scope.SoulbondPair -> SoulbondPairing.isInPairOf(state, permanentId, entityId)
                    is Scope.Battlefield ->
                        !(grant.filter.excludeSelf && permanentId == entityId) &&
                            predicateEvaluator.matches(state, projected, entityId, grant.filter.baseFilter, predicateContext)
                }
                if (covers) return true
            }
        }
        return false
    }
}
