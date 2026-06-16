package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ornithopter
 * {0}
 * Artifact Creature — Thopter
 * 0/2
 * Flying
 */
val Ornithopter = card("Ornithopter") {
    manaCost = "{0}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Thopter"
    power = 0
    toughness = 2
    oracleText = "Flying"
    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Amy Weber"
        flavorText = "Many scholars believe that these creatures were the result of Urza's first attempt at mechanical life, perhaps created in his early days as an apprentice to Tocasia."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59cc9bdb-7cf2-4795-bac7-ffff605c9eb0.jpg?1562913725"
    }
}
