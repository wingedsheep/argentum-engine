package com.wingedsheep.engine.handlers.effects.copy

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.identity.CardComponent

/** Saved identities predating frozen flip halves still carry their registered upright definition. */
fun CardComponent.withRestoredFlipSide(registry: CardRegistry): CardComponent {
    if (flipSide != null || ownerId == null) return this
    val definition = registry.getCard(cardDefinitionId) ?: return this
    if (definition.flipSide == null) return this
    return CardEntityFactory.applyDefinitionDecorations(ComponentContainer.of(this), definition)
        .get<CardComponent>()!!
}
