package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mirrodin Avenged
 * {B}
 * Instant
 * Destroy target creature that was dealt damage this turn.
 * Draw a card.
 */
val MirrodinAvenged = card("Mirrodin Avenged") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature that was dealt damage this turn.\nDraw a card."

    spell {
        val creature = target(TargetFilter.Creature.wasDealtDamageThisTurn())
        effect = Effects.Destroy(creature) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Scott Murphy"
        flavorText = "\"You were my mistake. I will forever bear the weight of your horrors, and of your end.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/5/750b2090-7fd4-4048-a148-9a5fc7b6f265.jpg?1783917004"
    }
}
