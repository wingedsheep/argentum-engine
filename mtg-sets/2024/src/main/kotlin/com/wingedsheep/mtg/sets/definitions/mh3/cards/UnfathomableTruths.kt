package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Unfathomable Truths — Modern Horizons 3 #77 (common)
 * {4}{U} · Instant
 *
 * Devoid (This card has no color.)
 * Draw three cards and create a 0/1 colorless Eldrazi Spawn creature token with
 * "Sacrifice this token: Add {C}."
 */
val UnfathomableTruths = card("Unfathomable Truths") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Draw three cards and create a 0/1 colorless Eldrazi Spawn creature token with " +
        "\"Sacrifice this token: Add {C}.\""

    keywords(Keyword.DEVOID)

    spell {
        effect = Effects.DrawCards(3) then Effects.CreateEldraziSpawn()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Drew Tucker"
        flavorText = "\"Maybe we are all just figments of Kozilek's imagination.\"\n—Anowon, the Ruin Sage"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f95b8eb1-4dbb-4bb9-aa31-b7a12e3b4618.jpg?1783911285"
    }
}
