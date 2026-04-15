package com.wingedsheep.engine.legalactions.support

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * A list of [LegalAction]s plus the [GameState] they were enumerated from.
 *
 * Carrying the state alongside the actions lets matchers resolve entity IDs
 * (the engine's currency) back to card names (the test author's currency)
 * without each call site needing to pass the state explicitly.
 */
class LegalActionsView(
    actions: List<LegalAction>,
    val state: GameState
) : List<LegalAction> by actions {

    /** The card name backing a CastSpell / PlayLand / ActivateAbility action, or null. */
    fun cardNameOf(action: GameAction): String? {
        val entityId: EntityId = when (action) {
            is CastSpell -> action.cardId
            is PlayLand -> action.cardId
            is ActivateAbility -> action.sourceId
            else -> return null
        }
        return state.getEntity(entityId)?.get<CardComponent>()?.name
    }

    fun castActions(): List<LegalAction> = filter { it.action is CastSpell }
    fun playLandActions(): List<LegalAction> = filter { it.action is PlayLand }
    fun activatedAbilityActions(): List<LegalAction> = filter { it.action is ActivateAbility }

    fun castActionsFor(cardName: String): List<LegalAction> =
        castActions().filter { cardNameOf(it.action) == cardName }

    fun playLandActionsFor(cardName: String): List<LegalAction> =
        playLandActions().filter { cardNameOf(it.action) == cardName }

    fun activatedAbilityActionsFor(sourceId: EntityId): List<LegalAction> =
        activatedAbilityActions().filter { (it.action as ActivateAbility).sourceId == sourceId }
}
