package com.wingedsheep.mtg.sets.definitions.all.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Deadly Insect
 * {4}{G}
 * Creature — Insect
 * 6 / 1
 */
val DeadlyInsect = card("Deadly Insect") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    oracleText = "Shroud (This creature can't be the target of spells or abilities.)"
    power = 6
    toughness = 1

    keywords(Keyword.SHROUD)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86a"
        artist = "Scott Kirschner"
        flavorText = "\"Beautiful, indeed—but one sting could fell a Giant in a heartbeat.\"\n" +
            "—Taaveti of Kelsinko, Elvish Hunter"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/add1b999-5c3f-4187-adac-ed1037406b3f.jpg"
    }
}
