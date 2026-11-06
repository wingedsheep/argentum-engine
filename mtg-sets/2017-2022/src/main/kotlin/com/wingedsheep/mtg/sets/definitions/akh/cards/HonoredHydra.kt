package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.embalm
import com.wingedsheep.sdk.model.Rarity

/**
 * Honored Hydra — Amonkhet #172
 * {5}{G} · Creature — Snake Hydra · Rare
 * 6/6
 * Trample
 * Embalm {3}{G}
 */
val HonoredHydra = card("Honored Hydra") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Hydra"
    power = 6
    toughness = 6
    oracleText = "Trample\n" +
        "Embalm {3}{G} ({3}{G}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a white Zombie Snake Hydra with no mana cost. Embalm only as a sorcery.)"

    keywords(Keyword.TRAMPLE)

    embalm("{3}{G}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Todd Lockwood"
        flavorText = "Even gods have pets."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21c33902-a06a-4321-bdf4-cea5494fda63.jpg?1783936473"
    }
}
