package com.wingedsheep.mtg.sets.definitions.trc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gorn Captain
 * {3}{G}
 * Creature — Gorn Soldier
 * 4/3
 * Trample
 */
val GornCaptain = card("Gorn Captain") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Gorn Soldier"
    oracleText = "Trample (This creature can deal excess combat damage to the player it's attacking.)"
    power = 4
    toughness = 3
    keywords(Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "172"
        artist = "Jason Pastrana"
        flavorText = "\"I weary of the chase. Wait for me. I shall be merciful and quick.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/6/061dc7da-c803-49eb-b50a-a0801f09b5c0.jpg?1785981152"
    }
}
