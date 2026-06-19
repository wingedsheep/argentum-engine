package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.state.components.identity.FaceDownTurnUpComponent
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.FaceDownMode

/**
 * Derives the [FaceDownTurnUpComponent] that lets a face-down permanent be turned face up, given the
 * card it represents and the [FaceDownMode] under which it entered the battlefield.
 *
 * This is the one place that knows the turn-up rule of each face-down mechanic. The turn-up itself
 * (special action, payment, flip) is mechanic-agnostic — it reads only [FaceDownTurnUpComponent] — so a
 * manifested creature reuses the entire morph turn-up machinery for free.
 *
 * Returns `null` when the permanent has no way to be turned face up (a non-morph card entered as
 * morph, a manifested non-creature card per CR 701.40b, or [FaceDownMode.HIDDEN]).
 */
object FaceDownTurnUp {

    fun dataFor(
        cardDef: CardDefinition?,
        cardDefinitionId: String,
        mode: FaceDownMode
    ): FaceDownTurnUpComponent? = when (mode) {
        // Morph / Megamorph (CR 702.37): turn face up by paying the card's morph cost.
        FaceDownMode.MORPH -> {
            val morph = cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Morph>()?.firstOrNull()
            morph?.let { FaceDownTurnUpComponent(it.morphCost, cardDefinitionId, it.faceUpEffect) }
        }

        // Manifest / Cloak (CR 701.40b): turn face up by paying the card's mana cost, but only if
        // the card representing the permanent is a creature card. A manifested non-creature card
        // can never be turned face up this way.
        FaceDownMode.MANIFEST ->
            if (cardDef != null && cardDef.typeLine.isCreature) {
                FaceDownTurnUpComponent(
                    turnUpCost = PayCost.Atom(CostAtom.Mana(cardDef.manaCost)),
                    originalCardDefinitionId = cardDefinitionId,
                    faceUpEffect = null
                )
            } else {
                null
            }

        // Exiled / hidden face down — no in-place turn-up procedure.
        FaceDownMode.HIDDEN -> null
    }
}
