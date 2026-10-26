package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId

/**
 * The card definition whose text this permanent has, or null when it has none to read.
 *
 * A face-down permanent has only the characteristics its face-down rule lists (CR 708.2): no name,
 * no text, none of its card's abilities. Its entity still carries the real [CardComponent], so a
 * scan that goes from the battlefield to `cardRegistry.getCard(card.cardDefinitionId)` reads a
 * card nobody may know — a rules bug, and a leak whenever the scan's result is visible (a manifested
 * Propaganda taxing attacks, a face-down Dueling Grounds capping blockers; #3340 group D).
 *
 * Use this instead of the registry lookup wherever a battlefield scan reads printed abilities.
 * Abilities granted to a face-down permanent are not printed and still apply; read those from
 * their own stores as before.
 */
fun ComponentContainer.faceUpCardDefinition(cardRegistry: CardRegistry): CardDefinition? {
    if (has<FaceDownComponent>()) return null
    val card = get<CardComponent>() ?: return null
    return cardRegistry.getCard(card.cardDefinitionId)
}

/** [ComponentContainer.faceUpCardDefinition] for the entity [entityId]. */
fun GameState.faceUpCardDefinition(entityId: EntityId, cardRegistry: CardRegistry): CardDefinition? =
    getEntity(entityId)?.faceUpCardDefinition(cardRegistry)
