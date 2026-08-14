package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LegendRuleContinuation
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LegendRuleDoesNotApplyTo

/**
 * 704.5j - Legend rule: If a player controls two or more legendary permanents
 * with the same name, that player chooses one and puts the rest into graveyard.
 */
class LegendRuleCheck(
    private val decisionHandler: DecisionHandler,
    private val cardRegistry: CardRegistry
) : StateBasedActionCheck {
    override val name = "704.5j Legend Rule"
    override val order = SbaOrder.LEGEND_RULE

    private val predicateEvaluator = PredicateEvaluator()

    /**
     * The filters of every [LegendRuleDoesNotApplyTo] static among [permanents] (a single player's
     * battlefield). Collected once per player so the per-legendary exemption test doesn't re-scan
     * the battlefield (Spider-Verse: "The 'legend rule' doesn't apply to Spiders you control").
     */
    private fun collectExemptionFilters(
        state: GameState,
        permanents: List<EntityId>
    ): List<GameObjectFilter> {
        val filters = mutableListOf<GameObjectFilter>()
        for (permId in permanents) {
            val cardDef = state.getEntity(permId)?.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) } ?: continue
            for (ability in cardDef.script.staticAbilities) {
                if (ability is LegendRuleDoesNotApplyTo) filters.add(ability.filter)
            }
        }
        return filters
    }

    /**
     * Whether [entityId] (controlled by [playerId]) is exempt from the legend rule because it
     * matches one of [exemptionFilters] — the "legend rule doesn't apply to [filter] you control"
     * statics that player controls. Short-circuits when there are none (the common case).
     */
    private fun isExemptFromLegendRule(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        entityId: EntityId,
        exemptionFilters: List<GameObjectFilter>
    ): Boolean {
        if (exemptionFilters.isEmpty()) return false
        val ctx = PredicateContext(controllerId = playerId)
        return exemptionFilters.any { filter ->
            predicateEvaluator.matches(state, projected, entityId, filter, ctx)
        }
    }

    override fun check(state: GameState): ExecutionResult {
        val projected = state.projectedState
        for (playerId in state.turnOrder) {
            val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
            val permanents = state.getZone(battlefieldZone)

            // Collect this player's legend-rule exemptions once, not per legendary permanent.
            val exemptionFilters = collectExemptionFilters(state, permanents)

            val legendaryByName = mutableMapOf<String, MutableList<EntityId>>()

            for (entityId in permanents) {
                val container = state.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue

                if (projected.isLegendary(entityId) &&
                    !isExemptFromLegendRule(state, projected, playerId, entityId, exemptionFilters)
                ) {
                    // Use the current (projected) name, not just the printed one: a Layer-3
                    // SetName continuous effect (e.g. Witness Protection, "named Legitimate
                    // Businessperson") can make two otherwise-distinct legendary permanents
                    // share a name (CR 201.2a: "objects have the same name if they have at
                    // least one name in common"), which triggers the legend rule (CR 704.5j).
                    val currentName = projected.getName(entityId) ?: cardComponent.name
                    legendaryByName.getOrPut(currentName) { mutableListOf() }.add(entityId)
                }
            }

            for ((name, entityIds) in legendaryByName) {
                if (entityIds.size > 1) {
                    val decisionResult = decisionHandler.createCardSelectionDecision(
                        state = state,
                        playerId = playerId,
                        sourceId = null,
                        sourceName = null,
                        prompt = "Choose which $name to keep (legend rule)",
                        options = entityIds,
                        minSelections = 1,
                        maxSelections = 1,
                        ordered = false,
                        phase = DecisionPhase.STATE_BASED,
                        useTargetingUI = true
                    )

                    val continuation = LegendRuleContinuation(
                        decisionId = decisionResult.pendingDecision!!.id,
                        playerId = playerId,
                        allDuplicates = entityIds
                    )

                    val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

                    return ExecutionResult.paused(
                        stateWithContinuation,
                        decisionResult.pendingDecision,
                        decisionResult.events
                    )
                }
            }
        }

        return ExecutionResult.success(state)
    }
}
