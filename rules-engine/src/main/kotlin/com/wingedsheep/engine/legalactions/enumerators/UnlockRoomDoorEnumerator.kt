package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.sdk.scripting.SpecialActionKind
import com.wingedsheep.engine.core.UnlockRoomDoor
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.state.components.identity.RoomComponent

/**
 * Enumerates door-unlock special actions for Room permanents the player controls
 * (CR 709.5e + rule 116). One action per locked face whose printed mana cost is payable.
 *
 * Timing: sorcery speed — controller's main phase, stack empty.
 */
class UnlockRoomDoorEnumerator : ActionEnumerator {

    // Lets restricted mana tagged "spend only to ... unlock a door" count toward affordability
    // of the unlock special action (Creeping Peeper).
    private val unlockContext = SpellPaymentContext(isUnlockDoorAction = true)

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        if (!context.canPlaySorcerySpeed) return emptyList()

        val state = context.state
        val playerId = context.playerId
        val result = mutableListOf<LegalAction>()

        for (entityId in context.battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue
            val room = container.get<RoomComponent>() ?: continue

            for (face in room.lockedFaces) {
                val cost = context.specialActionCostReducer.effectiveCost(state, playerId, SpecialActionKind.UNLOCK_DOOR, face.manaCost)
                if (!context.manaSolver.canPay(state, playerId, cost, spellContext = unlockContext, precomputedSources = context.availableManaSources)) {
                    continue
                }
                val autoTapPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, cost, spellContext = unlockContext, precomputedSources = context.availableManaSources)
                        ?.sources?.map { it.entityId }
                }
                result.add(
                    LegalAction(
                        actionType = "UnlockRoomDoor",
                        description = "Unlock ${face.name} (${cost})",
                        action = UnlockRoomDoor(playerId, entityId, face.id),
                        manaCostString = cost.toString(),
                        autoTapPreview = autoTapPreview
                    )
                )
            }
        }

        return result
    }
}
