package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CreatureStats

/**
 * Copy effects read face-down characteristics before the upright half of a flipped card.
 * Face-down values are copiable; flipped status itself is not (CR 707.2, 708.2a).
 * A separate definition id prevents ability lookups from exposing the hidden rules text.
 */
fun ComponentContainer.copiableCardComponent(): CardComponent? {
    val card = get<CardComponent>() ?: return null
    if (has<FaceDownComponent>()) {
        val mode = get<FaceDownModeComponent>()?.mode
        val ward = mode?.faceDownWard
        return CardComponent(
            cardDefinitionId = "face-down",
            name = "",
            manaCost = ManaCost.ZERO,
            typeLine = TypeLine.parse("Creature"),
            baseStats = CreatureStats(2, 2),
            baseKeywords = if (ward == null) emptySet() else setOf(Keyword.WARD),
            copyWardCosts = listOfNotNull(ward),
            ownerId = card.ownerId,
            imageUri = mode?.helperCardImageUri,
        )
    }
    // CR 712.8g: a copy of a melded permanent has mana value 0, not the summed front faces'.
    if (has<MeldedComponent>()) return card.copy(manaValueOverride = null)
    return get<FlippedComponent>()?.unflippedCard ?: card
}

/** A face-down double-faced source still makes a double-faced token, with only public values. */
fun ComponentContainer.copiableDoubleFacedComponent(
    copy: (CardComponent) -> CardComponent,
): DoubleFacedComponent? {
    val dfc = get<DoubleFacedComponent>() ?: return null
    val faces = if (has<FaceDownComponent>()) {
        val publicCard = copiableCardComponent() ?: return null
        CopiedCardFaces(copy(publicCard), copy(publicCard))
    } else {
        dfc.copiedFaces?.let { CopiedCardFaces(copy(it.front), copy(it.back)) }
    }
    return if (faces == null) dfc.copy(frontFaceCard = null, faceChanges = 0) else DoubleFacedComponent(
        frontCardDefinitionId = faces.front.cardDefinitionId,
        backCardDefinitionId = faces.back.cardDefinitionId,
        currentFace = if (has<FaceDownComponent>()) DoubleFacedComponent.Face.FRONT else dfc.currentFace,
        copiedFaces = faces,
    )
}
