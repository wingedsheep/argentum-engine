package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * A battlefield-entry replacement that sends a **token** somewhere else as it is created —
 * Hallowed Moonlight's "until end of turn, if a creature would enter and it wasn't cast, exile it
 * instead". Creating a token puts it onto the battlefield (CR 111.1), so every
 * [com.wingedsheep.sdk.scripting.RedirectZoneChange] whose `appliesTo` is a `to = BATTLEFIELD`
 * zone change applies to it exactly as it does to a card entering: the ruling on Hallowed
 * Moonlight is that a creature token "is put into exile instead and then ceases to exist" (the
 * tokens-in-other-zones state-based action, [com.wingedsheep.engine.mechanics.sba.zone.TokensInWrongZonesCheck]).
 * Containment Priest and Mistcaller are the same replacement with a `nontoken()` filter.
 *
 * Every token-creation path places its tokens through the ad-hoc
 * [com.wingedsheep.engine.handlers.effects.BattlefieldEntry.place] instead of the zone-change
 * pipeline, so each calls [divert] on a token that is fully built (identity, controller, any
 * granted entry copy applied — the entry is judged as the token would exist on the battlefield)
 * but not yet placed. A diverted token never enters: no entry tracker record, no enters-tapped or
 * enters-with-counters replacements, no as-enters choice, and no ETB triggers. Its creation emits a
 * [ZoneChangeEvent] from nowhere into the redirect's zone instead of onto the battlefield, and the
 * caller leaves it out of the instruction's created-token collection.
 *
 * Only the destination is honoured: a token that leaves for a hidden or public zone ceases to
 * exist at the next state-based-action check either way, so the shuffle / reveal / linked-exile
 * riders of a redirect have nothing to act on. No printed battlefield-entry redirect carries an
 * additional effect ([com.wingedsheep.sdk.scripting.RedirectZoneChangeWith]) today.
 */
internal object TokenEntryRedirect {

    /** The token sent elsewhere and the creation event that records where it went. */
    data class Diverted(val state: GameState, val event: ZoneChangeEvent)

    /**
     * When a battlefield-entry replacement sends [tokenId] elsewhere, put it there and return the
     * result; null when the token enters the battlefield as normal.
     *
     * @param preCreationState the state before this instruction created any token: tokens created
     *   simultaneously aren't on the battlefield yet to replace one another's entry (a token copy
     *   of Containment Priest made alongside a creature token doesn't exile it).
     */
    fun divert(
        state: GameState,
        tokenId: EntityId,
        preCreationState: GameState,
        predicateEvaluator: PredicateEvaluator,
    ): Diverted? {
        val redirect = ZoneMovementUtils.checkZoneChangeRedirect(
            state, tokenId, fromZone = null, toZone = Zone.BATTLEFIELD,
            battlefieldSourceState = preCreationState,
            predicateEvaluator = predicateEvaluator,
        )
        val destination = redirect.destinationZone
        if (destination == Zone.BATTLEFIELD) return null
        val container = state.getEntity(tokenId) ?: return null
        val card = container.get<CardComponent>() ?: return null
        // A token is owned by the player under whose control it was created (CR 111.2).
        val ownerId = card.ownerId ?: container.get<ControllerComponent>()?.playerId ?: return null
        val diverted = state
            .updateEntity(tokenId) { c ->
                c.without<ControllerComponent>()
                    .without<SummoningSicknessComponent>()
                    .without<EnteredThisTurnComponent>()
                    .without<AttackingComponent>()
            }
            .addToZone(ZoneKey(ownerId, destination), tokenId)
        return Diverted(
            diverted,
            ZoneChangeEvent(
                entityId = tokenId,
                entityName = card.name,
                fromZone = null,
                toZone = destination,
                ownerId = ownerId,
                oldObject = null,
                newObject = diverted.objectRef(tokenId),
            ),
        )
    }
}
