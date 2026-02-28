package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AmplifyEffect

/**
 * Zombie Brute
 * {6}{B}
 * Creature — Zombie
 * 5/4
 * Amplify 1 (As this creature enters, put a +1/+1 counter on it for each
 * Zombie card you reveal in your hand.)
 * Trample
 */
val ZombieBrute = card("Zombie Brute") {
    manaCost = "{6}{B}"
    typeLine = "Creature — Zombie"
    power = 5
    toughness = 4
    oracleText = "Amplify 1 (As this creature enters, put a +1/+1 counter on it for each Zombie card you reveal in your hand.)\nTrample"

    keywords(Keyword.AMPLIFY, Keyword.TRAMPLE)

    replacementEffect(AmplifyEffect(countersPerReveal = 1))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Greg Hildebrandt"
        flavorText = "Measure its determination by the screams of its prey."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b37db470-3aef-4fc4-98ce-63b5fb2546f6.jpg?1562931118"
    }
}
