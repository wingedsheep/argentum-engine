package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy

/**
 * Clone
 * {3}{U}
 * Creature — Shapeshifter
 * 0/0
 * You may have Clone enter the battlefield as a copy of any creature on the battlefield.
 */
val Clone = card("Clone") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Shapeshifter"
    power = 0
    toughness = 0
    oracleText = "You may have Clone enter the battlefield as a copy of any creature on the battlefield."

    replacementEffect(EntersAsCopy(optional = true))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Carl Critchlow"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d513dde-7c5f-46f1-b871-5290595bdbbe.jpg"
    }
}
