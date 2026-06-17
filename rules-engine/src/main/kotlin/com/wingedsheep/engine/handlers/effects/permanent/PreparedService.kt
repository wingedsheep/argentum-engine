package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.PreparedComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId

/**
 * Shared machinery for the Prepare mechanic (Secrets of Strixhaven).
 *
 * A permanent "becomes prepared" by gaining the [PreparedComponent] and having a copy of its
 * card's prepare spell ([CardDefinition.cardFaces] index 0) placed in its controller's exile,
 * with a permanent cast-from-exile permission. Casting that copy unprepares the creature.
 *
 * Two entry points use this:
 *  - the stack resolver, when a [com.wingedsheep.sdk.model.CardLayout.PREPARE] permanent enters
 *    the battlefield (it enters prepared); and
 *  - [MakePreparedEffectExecutor], when a triggered/resolution effect makes a creature become
 *    prepared mid-game (e.g. Joined Researchers' end-step trigger).
 *
 * Idempotent: a permanent that is already prepared is left untouched (CR — a creature that is
 * already prepared doesn't become prepared again, so no second copy is created).
 */
object PreparedService {

    /**
     * Make [permanentId] prepared using its [cardDef] (whose `cardFaces[0]` is the prepare spell),
     * with [controllerId] as the player who may cast the exiled copy. No-op if the permanent is
     * already prepared or the card has no prepare face.
     */
    fun makePrepared(
        state: GameState,
        permanentId: EntityId,
        cardDef: CardDefinition,
        controllerId: EntityId,
    ): GameState {
        val entity = state.getEntity(permanentId) ?: return state
        // Already prepared: don't create a second exile copy.
        if (entity.get<PreparedComponent>() != null) return state
        val sourceCard = entity.get<CardComponent>() ?: return state
        val prepareFace = cardDef.cardFaces.firstOrNull() ?: return state

        var newState = state
        val (copyId, stateWithCopy) = newState.newEntity()
        newState = stateWithCopy
        newState = newState.updateEntity(copyId) { c ->
            c.with(
                CardComponent(
                    cardDefinitionId = sourceCard.cardDefinitionId,
                    name = sourceCard.name,
                    manaCost = prepareFace.manaCost,
                    typeLine = prepareFace.typeLine,
                    oracleText = prepareFace.oracleText,
                    colors = prepareFace.manaCost.colors,
                    ownerId = controllerId,
                    spellEffect = prepareFace.script.spellEffect,
                    imageUri = sourceCard.imageUri,
                )
            ).with(
                CopyOfComponent(
                    originalCardDefinitionId = sourceCard.cardDefinitionId,
                    copiedCardDefinitionId = sourceCard.cardDefinitionId,
                )
            ).with(
                PreparedSpellCopyComponent(sourceId = permanentId)
            )
        }
        newState = newState.addToZone(ZoneKey(controllerId, Zone.EXILE), copyId)

        val (permId, stateWithPerm) = newState.newEntity()
        newState = stateWithPerm.addMayPlayPermission(
            MayPlayPermission(
                id = permId,
                cardIds = setOf(copyId),
                controllerId = controllerId,
                sourceId = permanentId,
                permanent = true,
                timestamp = newState.timestamp,
            )
        )

        newState = newState.updateEntity(permanentId) { c ->
            c.with(PreparedComponent(exileCopyId = copyId))
        }
        return newState
    }
}
