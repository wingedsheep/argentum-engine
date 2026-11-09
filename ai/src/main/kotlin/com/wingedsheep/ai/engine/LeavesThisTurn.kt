package com.wingedsheep.ai.engine

import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerStepScope
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Permanents a pending delayed trigger takes off the battlefield **later this turn** — the token
 * copy "sacrifice it at the beginning of the next end step" (Gray and Adira, Forever Joined,
 * Kiki-Jiki), a Mobilize warrior, a Sneak Attack creature, an "exile it at the next end step" copy.
 *
 * Such a creature is worth what it does in the fight still ahead of it and nothing after: it will
 * not attack, block or hold a card's static on any later turn. A combat score read off the
 * board straight after damage still sees it, so killing it read as killing a real creature, and
 * the AI gang-blocked a 5/6 token copy with four creatures — losing two of them — to kill a token
 * that was going to be sacrificed at the end step anyway (2026-11 AI-vs-humans report).
 * [AiProfile.departuresAreGone] carries the argument.
 *
 * Only the unconditional shapes are read: a bare sacrifice or zone move of one baked entity, alone
 * or inside a [CompositeEffect]. A gated departure ("exile it unless Sauron is your Ring-bearer")
 * may not happen, so it is not counted.
 */
internal object LeavesThisTurn {

    /** The battlefield permanents [state]'s pending delayed triggers remove before this turn ends. */
    fun permanents(state: GameState): Set<EntityId> {
        if (state.delayedTriggers.isEmpty()) return emptySet()
        val battlefield = state.getBattlefield().toSet()
        return state.delayedTriggers.asSequence()
            .filter { firesLaterThisTurn(state, it) }
            .flatMap { departing(it.effect) }
            .filter { it in battlefield }
            .toSet()
    }

    /**
     * [state] as it stands once those departures have happened: each permanent in [departing]
     * taken off the battlefield. Scoring only — no events, no dies triggers; the point is that the
     * score stops counting a body that will not be there.
     */
    fun without(state: GameState, departing: Set<EntityId>): GameState =
        departing.fold(state) { acc, id ->
            val owner = acc.turnOrder.firstOrNull { id in acc.getBattlefield(it) } ?: return@fold acc
            acc.removeFromZone(ZoneKey(owner, Zone.BATTLEFIELD), id)
        }

    /** [without] for every permanent [permanents] finds. */
    fun settled(state: GameState): GameState {
        val departing = permanents(state)
        return if (departing.isEmpty()) state else without(state, departing)
    }

    private fun firesLaterThisTurn(state: GameState, delayed: DelayedTriggeredAbility): Boolean {
        val step = delayed.fireAtStep ?: return false
        if (delayed.stepScope != DelayedTriggerStepScope.STEP) return false
        if (delayed.fireOnPlayerId != null && delayed.fireOnPlayerId != state.activePlayerId) return false
        if ((delayed.notBeforeTurn ?: 0) > state.turnNumber) return false
        return step.ordinal > state.step.ordinal
    }

    private fun departing(effect: Effect): List<EntityId> = when (effect) {
        is SacrificeTargetEffect -> listOfNotNull((effect.target as? EffectTarget.SpecificEntity)?.entityId)
        is MoveToZoneEffect -> if (effect.destination == Zone.BATTLEFIELD) emptyList()
            else listOfNotNull((effect.target as? EffectTarget.SpecificEntity)?.entityId)
        is CompositeEffect -> effect.effects.flatMap(::departing)
        else -> emptyList()
    }
}
