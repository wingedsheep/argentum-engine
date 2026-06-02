package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.MiscPatterns

/**
 * Dredge
 * {B}
 * Instant
 * Sacrifice a creature or land.
 * Draw a card.
 */
val Dredge = card("Dredge") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Sacrifice a creature or land.\nDraw a card."

    spell {
        effect = MiscPatterns.sacrifice(
            filter = GameObjectFilter.CreatureOrLand,
            then = Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68bfa3d5-0f0b-4684-9567-f1478da01df7.jpg?1562916105"
    }
}
