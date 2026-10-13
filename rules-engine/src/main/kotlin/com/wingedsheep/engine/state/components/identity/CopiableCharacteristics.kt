package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.sdk.scripting.KeywordAbility
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

/** Numeric ability values are copiable, while flipped status and face-down rules text are not. */
fun ComponentContainer.copiableNumericKeywords(registry: CardRegistry? = null): List<KeywordAbility.Numeric> =
    buildList {
        if (!has<FaceDownComponent>()) {
            val upright = get<FlippedComponent>()?.unflippedCard
            if (upright != null) {
                addAll(registry?.getCard(upright.cardDefinitionId)?.keywordAbilities
                    ?.filterIsInstance<KeywordAbility.Numeric>().orEmpty())
                addAll(upright.copyNumericKeywords)
            } else {
                get<ToxicComponent>()?.let { add(KeywordAbility.Numeric(Keyword.TOXIC, it.amount)) }
                get<NumericKeywordValuesComponent>()?.values?.forEach { (keyword, n) ->
                    add(KeywordAbility.Numeric(keyword, n))
                }
            }
        }
    }

/** Rebuild the departed object's copiable values without borrowing any later object's components. */
fun EntitySnapshot.copiableContainer(): ComponentContainer? {
    val card = copiableCard ?: return null
    var container = CardEntityFactory.applyNumericKeywords(ComponentContainer.of(card), copiableNumericKeywords)
    copiableDoubleFaced?.let { container = container.with(it) }
    return container
}

/**
 * The copiable values of the permanent [entityId] as they are right now, tagged with its battlefield
 * visit — what a trigger freezes about an object it captured, so a payoff can still copy it after
 * the object has left (CR 608.2h), including a token that has since ceased to exist (CR 704.5d).
 */
fun captureCopiableSnapshot(
    state: com.wingedsheep.engine.state.GameState,
    entityId: com.wingedsheep.sdk.model.EntityId,
    registry: CardRegistry,
): EntitySnapshot? {
    val container = state.getEntity(entityId) ?: return null
    val card = container.copiableCardComponent() ?: return null
    return EntitySnapshot(
        entityId = entityId,
        battlefieldEntryTimestamp = container
            .get<com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent>()?.timestamp,
        copiableCard = card,
        copiableNumericKeywords = container.copiableNumericKeywords(registry),
        copiableDoubleFaced = container.copiableDoubleFacedComponent { it },
    )
}
