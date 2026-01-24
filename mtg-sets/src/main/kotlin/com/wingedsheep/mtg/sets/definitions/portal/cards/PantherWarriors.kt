package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Panther Warriors
 * {4}{G}
 * Creature — Cat Warrior
 * 6/3
 */
val PantherWarriors = card("Panther Warriors") {
    manaCost = "{4}{G}"
    typeLine = "Creature — Cat Warrior"
    power = 6
    toughness = 3

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "180"
        artist = "Eric Peterson"
        flavorText = "Swift and deadly, they strike from the shadows."
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7dba5c6-70aa-4a59-8f1f-e45f3cae1d08.jpg"
    }
}
