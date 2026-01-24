package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Coral Eel
 * {1}{U}
 * Creature - Fish
 * 2/1
 */
val CoralEel = card("Coral Eel") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Fish"
    power = 2
    toughness = 1

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Una Fricker"
        flavorText = "\"Some fishers like to eat eels, and some eels like to eat fishers.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35bbb10f-c118-4905-8329-3963af415178.jpg"
    }
}
