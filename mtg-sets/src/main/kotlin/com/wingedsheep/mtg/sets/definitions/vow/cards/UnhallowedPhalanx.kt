package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Unhallowed Phalanx
 * {4}{B}
 * Creature — Zombie Soldier
 * 1/13
 * This creature enters tapped.
 */
val UnhallowedPhalanx = card("Unhallowed Phalanx") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Soldier"
    oracleText = "This creature enters tapped."
    power = 1
    toughness = 13
    replacementEffect(EntersTapped())
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Nicholas Gregory"
        flavorText = "\"In case any of you new recruits were wondering, this is why we don't use mass graves anymore.\"\n—Kerren of the Mausoleum Guards"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5ec541b-1799-4c0e-a3fb-c008cf2eb911.jpg?1782703092"
    }
}
