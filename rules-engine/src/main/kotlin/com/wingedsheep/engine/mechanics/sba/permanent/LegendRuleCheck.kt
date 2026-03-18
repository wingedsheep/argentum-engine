package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LegendRuleContinuation
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * 704.5j - Legend rule: If a player controls two or more legendary permanents
 * with the same name, that player chooses one and puts the rest into graveyard.
 */
class LegendRuleCheck(
    private val decisionHandler: DecisionHandler
) : StateBasedActionCheck {
    override val name = "704.5j Legend Rule"
    override val order = SbaOrder.LEGEND_RULE

    override fun check(state: GameState): ExecutionResult {
        val projected = state.projectedState
        for (playerId in state.turnOrder) {
            val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
            val permanents = state.getZone(battlefieldZone)

            val legendaryByName = mutableMapOf<String, MutableList<EntityId>>()

            for (entityId in permanents) {
                val container = state.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue

                if (projected.isLegendary(entityId)) {
                    legendaryByName.getOrPut(cardComponent.name) { mutableListOf() }.add(entityId)
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
