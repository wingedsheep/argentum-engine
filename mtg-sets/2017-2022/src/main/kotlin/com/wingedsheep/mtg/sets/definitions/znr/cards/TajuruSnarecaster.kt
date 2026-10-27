package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tajuru Snarecaster — Zendikar Rising #210
 * {2}{G} · Creature — Elf Rogue · 1/4
 *
 * Reach
 */
val TajuruSnarecaster = card("Tajuru Snarecaster") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Rogue"
    oracleText = "Reach"
    power = 1
    toughness = 4

    keywords(Keyword.REACH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "210"
        artist = "Rudy Siswanto"
        flavorText = "Elves of the Tajuru Nation spend their lives in the ancient jaddi trees' expansive canopy, " +
            "some living and dying without ever touching the ground."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1894cf9-7d53-4b7e-aaae-8db42bdd8e49.jpg"
    }
}
