package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.legalactions.utils.CostEnumerationUtils
import com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Shared precomputed state for a single enumeration pass.
 *
 * Cross-cutting values are computed lazily at most once. All enumerators
 * share this context, avoiding redundant computation.
 */
class EnumerationContext(
    val state: GameState,
    val playerId: EntityId,
    val cardRegistry: CardRegistry,
    val manaSolver: ManaSolver,
    val costCalculator: CostCalculator,
    val predicateEvaluator: PredicateEvaluator,
    val conditionEvaluator: ConditionEvaluator,
    val turnManager: TurnManager
) {
    // Utility classes (lazy initialized)
    val targetUtils by lazy { TargetEnumerationUtils(predicateEvaluator) }
    val costUtils by lazy { CostEnumerationUtils(manaSolver, costCalculator, predicateEvaluator, cardRegistry) }
    val castPermissionUtils by lazy { CastPermissionUtils(cardRegistry, predicateEvaluator, conditionEvaluator) }

    // Projected state
    val projected: ProjectedState by lazy { state.projectedState }

    // Battlefield permanents controlled by player (via projected state)
    val battlefieldPermanents: List<EntityId> by lazy {
        projected.getBattlefieldControlledBy(playerId)
    }

    // Timing flags
    val canPlaySorcerySpeed: Boolean by lazy {
        state.step.isMainPhase && state.stack.isEmpty() && state.activePlayerId == playerId
    }

    // Land drop availability
    val canPlayLand: Boolean by lazy {
        val landDrops = state.getEntity(playerId)?.get<LandDropsComponent>()
        canPlaySorcerySpeed && (landDrops?.canPlayLand ?: false)
    }

    // Cast restrictions
    val cantCastSpells: Boolean by lazy {
        state.getEntity(playerId)?.has<CantCastSpellsComponent>() == true
    }

    // Alternative casting costs from battlefield permanents (e.g., Jodah)
    val alternativeCastingCosts by lazy {
        costCalculator.findAlternativeCastingCosts(state, playerId)
    }

    // Cycling prevention
    val cyclingPrevented by lazy {
        castPermissionUtils.isCyclingPrevented(state)
    }
}
