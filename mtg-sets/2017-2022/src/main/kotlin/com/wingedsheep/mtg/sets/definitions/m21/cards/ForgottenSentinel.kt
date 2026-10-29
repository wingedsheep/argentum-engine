package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Forgotten Sentinel
 * {4}
 * Artifact Creature — Golem
 * 4/3
 *
 * This creature enters tapped.
 */
val ForgottenSentinel = card("Forgotten Sentinel") {
    manaCost = "{4}"
    typeLine = "Artifact Creature — Golem"
    power = 4
    toughness = 3
    oracleText = "This creature enters tapped."

    replacementEffect(EntersTapped())

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "231"
        artist = "Joe Slucher"
        flavorText = "It was built to withstand the ages, but no one remembers who built it or why it keeps its eternal watch."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bd39a06-c53a-42c2-b2df-028358f03406.jpg?1783930657"
    }
}
