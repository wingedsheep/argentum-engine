package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.effects.CompositeEffect

/**
 * Big Score
 * {3}{R}
 * Instant
 *
 * As an additional cost to cast this spell, discard a card.
 * Draw two cards and create two Treasure tokens.
 */
val BigScore = card("Big Score") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, discard a card.\nDraw two cards and create two Treasure tokens. (They're artifacts with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    additionalCost(AdditionalCost.DiscardCards())

    spell {
        effect = CompositeEffect(
            listOf(
                Effects.DrawCards(2),
                Effects.CreateTreasure(count = 2)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/243b460e-e67a-40d7-a874-a61fdb011c5a.jpg?1721429138"
    }
}
