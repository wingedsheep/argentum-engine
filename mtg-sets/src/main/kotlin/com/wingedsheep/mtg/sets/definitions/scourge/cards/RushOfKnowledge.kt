package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.model.Rarity

/**
 * Rush of Knowledge
 * {4}{U}
 * Sorcery
 * Draw cards equal to the greatest mana value among permanents you control.
 */
val RushOfKnowledge = card("Rush of Knowledge") {
    manaCost = "{4}{U}"
    typeLine = "Sorcery"
    oracleText = "Draw cards equal to the greatest mana value among permanents you control."

    spell {
        effect = Effects.DrawCards(DynamicAmounts.battlefield(Player.You).maxManaValue())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "Eric Peterson"
        flavorText = "\"Limitless power is glorious until you gain limitless understanding.\"\n—Ixidor"
        imageUri = "https://cards.scryfall.io/large/front/6/5/65b03b40-671f-4973-8d75-c3fa878ef603.jpg?1562529750"
    }
}
