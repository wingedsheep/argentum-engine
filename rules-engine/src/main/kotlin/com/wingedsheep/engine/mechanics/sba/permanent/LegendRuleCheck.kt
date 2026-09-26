package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LegendRuleContinuation
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
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
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LegendRuleDoesNotApplyTo
import com.wingedsheep.sdk.scripting.StaticAbility

/**
 * 704.5j - Legend rule: If a player controls two or more legendary permanents
 * with the same name, that player chooses one and puts the rest into graveyard.
 */
class LegendRuleCheck(
    private val decisionHandler: DecisionHandler,
    private val cardRegistry: CardRegistry,
    private val predicateEvaluator: PredicateEvaluator
) : StateBasedActionCheck {
    override val name = "704.5j Legend Rule"
    override val order = SbaOrder.LEGEND_RULE

    /**
     * The filters of every [LegendRuleDoesNotApplyTo] static that applies to [playerId]'s
     * [permanents] (a single player's battlefield). Collected once per player so the per-legendary
     * exemption test doesn't re-scan the battlefield. Two sources:
     *  - printed statics on those permanents (Spider-Verse: "The 'legend rule' doesn't apply to
     *    Spiders you control");
     *  - durational grants in [GameState.grantedStaticAbilities] anchored to the player or to one of
     *    those permanents — the one-shot, turn-scoped form (Hall of Echoes: "The 'legend rule'
     *    doesn't apply to permanents you control this turn" = a player-anchored
     *    `GrantStaticAbility(LegendRuleDoesNotApplyTo(Permanent), Controller, EndOfTurn)`, which
     *    outlives the permanent that created it and expires in the cleanup step).
     *
     * Either form may sit behind a [ConditionalStaticAbility]; it counts only while its condition
     * holds now (Brothers Yamazaki: "If there are exactly two permanents named Brothers Yamazaki on
     * the battlefield, the 'legend rule' doesn't apply to them" — a third copy switches the
     * exemption off and the rule sees all three).
     */
    private fun collectExemptionFilters(
        state: GameState,
        playerId: EntityId,
        permanents: List<EntityId>
    ): List<GameObjectFilter> {
        val filters = mutableListOf<GameObjectFilter>()
        for (permId in permanents) {
            val cardDef = state.getEntity(permId)?.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) } ?: continue
            for (ability in cardDef.script.staticAbilities) {
                activeExemption(state, ability, permId, playerId)?.let { filters.add(it.filter) }
            }
        }
        if (state.grantedStaticAbilities.isNotEmpty()) {
            val holders = permanents.toSet() + playerId
            for (grant in state.grantedStaticAbilities) {
                if (grant.entityId !in holders) continue
                activeExemption(state, grant.ability, grant.sourceId ?: grant.entityId, playerId)
                    ?.let { filters.add(it.filter) }
            }
        }
        return filters
    }

    /**
     * Unwrap [raw] to a live [LegendRuleDoesNotApplyTo] — directly, or behind a
     * [ConditionalStaticAbility] whose condition holds for [sourceId] controlled by [controllerId] —
     * else `null`.
     */
    private fun activeExemption(
        state: GameState,
        raw: StaticAbility,
        sourceId: EntityId,
        controllerId: EntityId
    ): LegendRuleDoesNotApplyTo? = when (raw) {
        is LegendRuleDoesNotApplyTo -> raw
        is ConditionalStaticAbility -> (raw.ability as? LegendRuleDoesNotApplyTo)?.takeIf {
            predicateEvaluator.conditions.evaluate(
                state,
                raw.condition,
                EffectContext(sourceId = sourceId, controllerId = controllerId)
            )
        }
        else -> null
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
            val exemptionFilters = collectExemptionFilters(state, playerId, permanents)

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
                        useTargetingUI = true,
                        answer = LegendRuleContinuation(
                            playerId = playerId,
                            allDuplicates = entityIds
                        ),
                    )

                    return ExecutionResult.propagatePause(
                        decisionResult.state,
                        decisionResult.events
                    )
                }
            }
        }

        return ExecutionResult.success(state)
    }
}
