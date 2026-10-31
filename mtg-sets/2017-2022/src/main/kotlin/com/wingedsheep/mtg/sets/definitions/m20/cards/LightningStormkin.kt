package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lightning Stormkin (M20 #213)
 * {U}{R}  Creature — Elemental Wizard  2/2
 *
 * Flying
 * Haste
 */
val LightningStormkin = card("Lightning Stormkin") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Elemental Wizard"
    oracleText = "Flying\n" +
        "Haste (This creature can attack and {T} as soon as it comes under your control.)"
    power = 2
    toughness = 2

    keywords(Keyword.FLYING, Keyword.HASTE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Rudy Siswanto"
        flavorText = "\"I strike in the same place as many times as necessary.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43c6db4e-cc37-4473-b0f5-48d8a0b82f33.jpg"
    }
}
