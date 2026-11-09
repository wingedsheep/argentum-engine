package com.wingedsheep.ai.engine

import com.wingedsheep.engine.mechanics.durations.GrantDurationGate
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.NotedExileComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration

/**
 * "You may choose not to untap [this] during your untap step" (`AbilityFlag.MAY_NOT_UNTAP`): which
 * permanents are worth leaving tapped.
 *
 * Staying tapped is never worth anything on its own — the permanent can't attack, block or be
 * activated this turn. It pays only while something the permanent already started is riding on it:
 *
 *  - a continuous effect or granted ability it is the source of, lasting "for as long as [it]
 *    remains tapped" (Lwaxana Troi's trigger doubler, a Courier's pump, Old Man of the Sea's
 *    theft, Entrancing Lyre's lock), still applying to a permanent on the battlefield;
 *  - a card it exiled that comes back when it untaps (Tawnos's Coffin), when that card belongs to
 *    an opponent — our own creature we'd rather have back.
 *
 * With nothing riding on it, the permanent untaps. The generic "choose up to N" ranking used to
 * answer this question and, under `selectionCountsByValue`, kept every option tapped: a Lwaxana
 * that had never activated stayed tapped forever. A storage land's upkeep counter is not read here
 * — it can only spend its counters by tapping, so leaving it tapped for good was no better.
 */
object KeepTappedChoice {

    fun worthKeepingTapped(state: GameState, permanentId: EntityId, playerId: EntityId): Boolean =
        sustainsTappedEffect(state, permanentId) || holdsOpponentsExiledCard(state, permanentId, playerId)

    private fun Duration.endsWhenSourceUntaps(): Boolean =
        this is Duration.WhileSourceTapped ||
            this is Duration.WhileSourceTappedAndAffectedPowerAtMostSource ||
            this is Duration.WhileYouControlSourceAndSourceTapped

    private fun sustainsTappedEffect(state: GameState, sourceId: EntityId): Boolean {
        val battlefield = state.getBattlefield().toSet()
        val floating = state.floatingEffects.any { effect ->
            effect.sourceId == sourceId && effect.duration.endsWhenSourceUntaps() &&
                GrantDurationGate.sourceTapped(state, effect) &&
                effect.effect.affectedEntities.any { it in battlefield }
        }
        if (floating) return true
        fun grantLive(entityId: EntityId, grantSource: EntityId?, duration: Duration) =
            grantSource == sourceId && duration.endsWhenSourceUntaps() && entityId in battlefield &&
                GrantDurationGate.holds(state, entityId, grantSource, duration)
        return state.grantedStaticAbilities.any { grantLive(it.entityId, it.sourceId, it.duration) } ||
            state.grantedTriggeredAbilities.any { grantLive(it.entityId, it.sourceId, it.duration) } ||
            state.grantedActivatedAbilities.any { grantLive(it.entityId, it.sourceId, it.duration) }
    }

    private fun holdsOpponentsExiledCard(state: GameState, sourceId: EntityId, playerId: EntityId): Boolean {
        val principal = state.getEntity(sourceId)?.get<NotedExileComponent>()?.principalId ?: return false
        val owner = state.getEntity(principal)?.get<OwnerComponent>()?.playerId ?: return false
        return principal in state.getZone(owner, Zone.EXILE) && state.isOpponentTo(owner, playerId)
    }
}
