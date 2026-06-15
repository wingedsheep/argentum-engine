package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Yotian Soldier
 * {3}
 * Artifact Creature — Soldier
 * 1/4
 * Vigilance
 */
val YotianSoldier = card("Yotian Soldier") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Soldier"
    power = 1
    toughness = 4
    oracleText = "Vigilance"
    keywords(Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "79"
        artist = "Christopher Rush"
        flavorText = "After Kroog was destroyed while most of its defenders were at his side, Urza vowed that none of his allies would ever need to fear for their own defense again, even while laying siege to a city far from their homes."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27cf53e3-76f6-4831-800e-1259394d779d.jpg?1562903450"
    }
}
