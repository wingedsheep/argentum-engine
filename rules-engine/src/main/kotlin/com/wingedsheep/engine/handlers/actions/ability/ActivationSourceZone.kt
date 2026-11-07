package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.components.stack.captureLastKnown
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * The zone an ability's source was in as it was activated (CR 602.2) — read off the state before
 * the activation's costs were paid, so a cost that moves the source doesn't change the answer.
 */
internal fun activationZone(stateBeforeActivation: GameState, sourceId: EntityId): Zone? =
    if (sourceId in stateBeforeActivation.getBattlefield()) Zone.BATTLEFIELD
    else stateBeforeActivation.logicalZone(sourceId)?.zoneType

/**
 * The source as it last existed in [activatedFrom], when paying the activation's costs took it out
 * of that zone (CR 113.7a): a self-sacrificed permanent, a graveyard card exiled to pay for its own
 * ability. Null when the source is still where it was activated from.
 *
 * A card outside the battlefield and the stack has no controller; CR 108.4a has anything asking for
 * one use its owner instead, so the snapshot of a graveyard or hand card answers "you control" for
 * its owner just as the live card's ability does.
 */
internal fun activationSourceLastKnown(
    stateBeforeActivation: GameState,
    stateAfterCosts: GameState,
    sourceId: EntityId,
    activatedFrom: Zone?,
    battlefieldSnapshot: () -> EntitySnapshot?,
): EntitySnapshot? {
    if (activatedFrom == null) return null
    if (activatedFrom == Zone.BATTLEFIELD) {
        if (sourceId in stateAfterCosts.getBattlefield()) return null
        return battlefieldSnapshot() ?: captureLastKnown(stateBeforeActivation, sourceId)
    }
    if (activationZone(stateAfterCosts, sourceId) == activatedFrom) return null
    // Off the battlefield there is no projection entry, so the characteristics are the card's own
    // (the same reading the live off-battlefield matcher makes): colours, types and keywords.
    val snapshot = captureLastKnown(stateBeforeActivation, sourceId)
    val card = stateBeforeActivation.getEntity(sourceId)?.get<CardComponent>() ?: return snapshot
    return snapshot.copy(
        controllerId = snapshot.controllerId ?: snapshot.ownerId,
        typeLine = card.typeLine,
        subtypes = card.typeLine.subtypes.map { it.value }.toSet(),
        supertypes = card.typeLine.supertypes.map { it.name }.toSet(),
        colors = card.colors.map { it.name }.toSet(),
        keywords = card.baseKeywords.map { it.name }.toSet(),
    )
}
