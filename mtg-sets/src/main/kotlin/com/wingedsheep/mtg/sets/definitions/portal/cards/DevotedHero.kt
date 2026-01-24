package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Devoted Hero
 * {W}
 * Creature — Elf Soldier
 * 1/2
 * (No abilities - vanilla creature)
 */
val DevotedHero = card("Devoted Hero") {
    manaCost = "{W}"
    typeLine = "Creature — Elf Soldier"
    power = 1
    toughness = 2

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "DiTerlizzi"
        flavorText = "The heart's courage is the soul's guardian."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89fb4bf8-51b6-44a8-92ac-e5aec4e4f2bc.jpg"
    }
}
