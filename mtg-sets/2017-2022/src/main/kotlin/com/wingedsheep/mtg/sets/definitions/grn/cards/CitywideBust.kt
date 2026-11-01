package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Citywide Bust
 * {1}{W}{W}
 * Sorcery
 * Destroy all creatures with toughness 4 or greater.
 */
val CitywideBust = card("Citywide Bust") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Destroy all creatures with toughness 4 or greater."

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.Creature.toughnessAtLeast(4))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Victor Adame Minguez"
        flavorText = "\"Oh, you fellas are going to love the lockup. Excellent gruel. Very low ceilings.\"\n—Libuse, Boros sergeant"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a995200f-1e9d-4ff3-9e04-4a4309e0e09c.jpg?1783934204"
    }
}
