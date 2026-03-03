package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sagu Archer
 * {4}{G}
 * Creature — Snake Archer
 * 2/5
 * Reach
 * Morph {4}{G}
 */
val SaguArcher = card("Sagu Archer") {
    manaCost = "{4}{G}"
    typeLine = "Creature — Snake Archer"
    power = 2
    toughness = 5
    oracleText = "Reach\nMorph {4}{G}"

    keywords(Keyword.REACH)
    morph = "{4}{G}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "146"
        artist = "Steven Belledin"
        flavorText = "\"His arrows whistle like a serpent's hiss.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a26baca-467e-4d94-873e-f266bebd5fd8.jpg?1562785028"
    }
}
