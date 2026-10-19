package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.replacement.PendingReplacementRider
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PutOntoBattlefieldThen
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Applies [PutOntoBattlefieldThen] — "if a player would put a land onto the battlefield, that
 * player instead puts it onto the battlefield, then …" (Land Equilibrium) — to a permanent that has
 * just been placed on the battlefield.
 *
 * Like [EnterTappedReplacements], the replacement is stamped into its source's
 * [ReplacementEffectSourceComponent] and consulted from the battlefield whenever some *other*
 * permanent enters. The entry seams (PlayLandHandler, [ZoneTransitionService], token minting) call
 * [queue] once the entering permanent carries its controller and characteristics. The rest of the
 * replacement can ask its player a question (which land to sacrifice), which an entry seam has
 * nowhere to pause for, so it is queued as a [PendingReplacementRider]: drained at the end of the
 * putting effect, or at the settle boundary for a land play — in both cases before state-based
 * actions, trigger detection and priority, as part of the same replacement.
 */
object EntryRiderReplacements {

    /**
     * Queue the rider of every [PutOntoBattlefieldThen] on the battlefield that applies to
     * [enteringEntityId], put there by [putterId].
     *
     * The putter and condition gates read the board as it was *before* the entry — "an opponent who
     * controls at least as many lands as you do would put a land" is asked before the land arrives —
     * by evaluating them against [state] with the entering permanent taken off the battlefield.
     * The filter reads the entering permanent as it is entering (its controller and face).
     *
     * [predicateEvaluator] may be built without a card registry (the entry tracker has none), so a
     * filter or gate that reads a printed script — rather than components, counts and projected
     * characteristics — fails closed here.
     */
    fun queue(
        state: GameState,
        enteringEntityId: EntityId,
        putterId: EntityId,
        predicateEvaluator: PredicateEvaluator
    ): GameState {
        var riders: List<PendingReplacementRider>? = null
        for (sourceId in state.getBattlefield()) {
            if (sourceId == enteringEntityId) continue
            val container = state.getEntity(sourceId) ?: continue
            val replacements = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = state.projectedState.getController(sourceId) ?: continue
            for (effect in replacements.replacementEffects) {
                if (effect !is PutOntoBattlefieldThen) continue
                if (!matchesPutter(state, effect.putter, putterId, sourceControllerId)) continue
                if (!matchesEnterFilter(effect.appliesTo, enteringEntityId, sourceId, sourceControllerId, state, predicateEvaluator)) continue
                val gate = effect.condition
                if (gate != null) {
                    val context = EffectContext(
                        sourceId = sourceId,
                        controllerId = sourceControllerId,
                        triggeringPlayerId = putterId,
                    )
                    if (!predicateEvaluator.conditions.evaluate(withoutEntering(state, enteringEntityId), gate, context)) continue
                }
                riders = (riders ?: emptyList()) + PendingReplacementRider(
                    effect = effect.effect,
                    hostId = sourceId,
                    controllerId = sourceControllerId,
                    subjectId = putterId
                )
            }
        }
        return riders?.let { state.copy(pendingReplacementRiders = state.pendingReplacementRiders + it) } ?: state
    }

    /** Whether [putterId] is the [putter] relation to the replacement's controller (CR 102.3 opponents). */
    private fun matchesPutter(state: GameState, putter: Player, putterId: EntityId, sourceControllerId: EntityId): Boolean =
        when (putter) {
            Player.You -> putterId == sourceControllerId
            Player.EachOpponent -> state.isOpponentOf(putterId, sourceControllerId)
            Player.Each, Player.Any -> true
            else -> false
        }

    /**
     * [state] as it was before the entry event that brought [entityId], for the pre-entry gates:
     * without it, and without every permanent entering in the same simultaneous event (two lands
     * fetched by one search arrive together, so neither is on the board when the other would enter).
     */
    private fun withoutEntering(state: GameState, entityId: EntityId): GameState {
        val group = state.getEntity(entityId)?.get<BattlefieldEntryTimestampComponent>()?.simultaneousEntryGroup
        val entering = if (group == null) setOf(entityId) else state.getBattlefield().filterTo(mutableSetOf(entityId)) {
            state.getEntity(it)?.get<BattlefieldEntryTimestampComponent>()?.simultaneousEntryGroup == group
        }
        return state.copy(zones = state.zones.mapValues { (key: ZoneKey, ids) ->
            if (key.zoneType == Zone.BATTLEFIELD && ids.any { it in entering }) ids - entering else ids
        })
    }

    private fun matchesEnterFilter(
        event: EventPattern,
        enteringEntityId: EntityId,
        replacementSourceId: EntityId,
        sourceControllerId: EntityId,
        state: GameState,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        if (event !is EventPattern.ZoneChangeEvent) return false
        if (event.to != Zone.BATTLEFIELD) return false
        val predicateContext = PredicateContext(sourceId = replacementSourceId, controllerId = sourceControllerId)
        return predicateEvaluator.matches(state, state.projectedState, enteringEntityId, event.filter, predicateContext)
    }
}
