package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.PermanentsEnteredUnderControlThisTurnComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.EntersWithChoice

/**
 * "This enters under the control of an opponent of your choice" —
 * [EntersWithChoice.chosenOpponentControls] (Captive Audience). The opponent is chosen with the
 * card's other as-enters choices, before it enters (CR 614.12a), and is then the player it enters
 * under: the spell, effect and direct-placement entry seams all read the controller from here.
 */
object ChosenEntryController {

    /** Whether [cardDef] enters under the control of the opponent it records in [ChoiceSlot.OPPONENT]. */
    fun appliesTo(cardDef: CardDefinition?): Boolean =
        cardDef?.script?.replacementEffects?.any { it is EntersWithChoice && it.chosenOpponentControls } == true

    /**
     * The player [cardDef]'s entrant enters under, read from its as-enters [choices], or null when
     * it enters under its usual controller (no such replacement, or no opponent was chosen).
     */
    fun of(cardDef: CardDefinition?, choices: Map<ChoiceSlot, ChoiceValue>?): EntityId? =
        if (!appliesTo(cardDef)) null
        else (choices?.get(ChoiceSlot.OPPONENT) as? ChoiceValue.EntityChoice)?.entityId

    /**
     * Hand a permanent that was placed on [from]'s battlefield before its as-enters choices were
     * answered (a token, a land played, a leyline) to [to], as if it had entered there: its
     * battlefield zone, base controller and this turn's "entered under your control" record all
     * move. Nothing has seen the entry yet — its [com.wingedsheep.engine.core.ZoneChangeEvent] is
     * emitted after the last choice — so this is part of the entry, not a control change.
     */
    fun enterUnder(state: GameState, entityId: EntityId, from: EntityId, to: EntityId): GameState {
        if (from == to) return state
        var newState = state.removeFromZone(ZoneKey(from, Zone.BATTLEFIELD), entityId)
            .addToZone(ZoneKey(to, Zone.BATTLEFIELD), entityId)
            .updateEntity(entityId) { it.with(ControllerComponent(to)) }
        val record = newState.getEntity(from)?.get<PermanentsEnteredUnderControlThisTurnComponent>()
            ?.entries?.lastOrNull { it.entityId == entityId } ?: return newState
        newState = newState.updateEntity(from) { c ->
            val log = c.get<PermanentsEnteredUnderControlThisTurnComponent>() ?: return@updateEntity c
            c.with(log.copy(entries = log.entries - record))
        }
        return newState.updateEntity(to) { c ->
            val log = c.get<PermanentsEnteredUnderControlThisTurnComponent>()
                ?: PermanentsEnteredUnderControlThisTurnComponent()
            c.with(log.copy(entries = log.entries + record))
        }
    }
}
