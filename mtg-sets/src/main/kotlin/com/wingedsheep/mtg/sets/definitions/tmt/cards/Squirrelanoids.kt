package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Squirrelanoids
 * {B}
 * Creature — Squirrel Mutant
 * 1/1
 *
 * Deathtouch
 */
val Squirrelanoids = card("Squirrelanoids") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Squirrel Mutant"
    oracleText = "Deathtouch"
    power = 1
    toughness = 1

    keywords(Keyword.DEATHTOUCH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Eilene Cherie"
        flavorText = "It's best to never discover what they squirrel away."
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be08d2b0-375b-434f-9e6d-060809e0ed34.jpg?1771586916"
    }
}
