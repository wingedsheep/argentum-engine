package com.wingedsheep.engine.mechanics

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardLayout

/**
 * Which `cardFaces` entries of a card are castable as a spell of their own wherever the card
 * itself may be cast: an Adventure (CR 715.3), an Omen (CR 720.3) and a modal DFC's spell face
 * (CR 712.11b). Split halves (cast through their own offers), prepare spells (only a prepared copy
 * is cast as one) and every other layout's faces are not, and neither is a land face, which is
 * played rather than cast (CR 305.1). A modal DFC's *permanent* back lives in `backFace` and is
 * [ModalDfcCasts]' business.
 *
 * Shared by the from-zone cast enumerator, the cast validator's may-play face check and the
 * granted-foretell face pricing, so what is offered, accepted and priced is one set.
 */
object AlternativeFaceCasts {

    private val LAYOUTS = setOf(CardLayout.ADVENTURE, CardLayout.OMEN, CardLayout.MODAL_DFC)

    /** The indices of [cardDef]'s castable spell faces, or empty when it has none. */
    fun spellFaceIndices(cardDef: CardDefinition?): List<Int> {
        if (cardDef == null || cardDef.layout !in LAYOUTS) return emptyList()
        return cardDef.cardFaces.indices.filterNot { cardDef.cardFaces[it].typeLine.isLand }
    }
}
