package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Galewind Moose
 * {4}{G}{G}
 * Creature — Elemental Elk
 * 6/6
 *
 * Flash
 * Vigilance, reach, trample
 */
val GalewindMoose = card("Galewind Moose") {
    manaCost = "{4}{G}{G}"
    typeLine = "Creature — Elemental Elk"
    power = 6
    toughness = 6
    oracleText = "Flash\nVigilance, reach, trample"

    keywords(Keyword.FLASH, Keyword.VIGILANCE, Keyword.REACH, Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "173"
        artist = "Valera Lutfullina"
        flavorText = "\"Something has stirred the Calamity Beasts, and they are wreaking havoc. Anyone who wishes to help, come forth! Whether it's with your swords in the fight or your shovels in the field, join me!\"\n—Mabel, heir to Cragflame"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58706bd8-558a-43b9-9f1e-c1ff0044203b.jpg?1721426814"
    }
}
