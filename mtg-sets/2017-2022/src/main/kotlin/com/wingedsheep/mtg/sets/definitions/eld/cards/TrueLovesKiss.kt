package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * True Love's Kiss
 * {2}{W}{W}
 * Instant
 *
 * Exile target artifact or enchantment.
 * Draw a card.
 */
val TrueLovesKiss = card("True Love's Kiss") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target artifact or enchantment.\nDraw a card."

    spell {
        val t = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Exile(t) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Donato Giancola"
        flavorText = "\"Be careful, dear. Some people deserve their curses.\"\n—Marawen, barrow witch"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23a4bac2-f6cb-4712-8510-a63657c43a5c.jpg?1783932663"
    }
}
