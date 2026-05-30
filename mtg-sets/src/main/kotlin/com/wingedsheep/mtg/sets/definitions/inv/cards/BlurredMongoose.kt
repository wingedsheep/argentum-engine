package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Blurred Mongoose
 * {1}{G}
 * Creature — Mongoose
 * 2/1
 * This spell can't be countered.
 * Shroud (This creature can't be the target of spells or abilities.)
 */
val BlurredMongoose = card("Blurred Mongoose") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Mongoose"
    power = 2
    toughness = 1
    oracleText = "This spell can't be countered.\n" +
        "Shroud (This creature can't be the target of spells or abilities.)"

    cantBeCountered = true
    keywords(Keyword.SHROUD)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "183"
        artist = "Heather Hudson"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b073e3f-6a6f-495a-ab16-39d906b660f1.jpg?1562910191"
    }
}
