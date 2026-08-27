package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Iroas's Champion
 * {1}{R}{W}
 * Creature — Human Soldier
 * 2/2
 * Double strike
 */
val IroassChampion = card("Iroas's Champion") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Double strike (This creature deals both first-strike and regular combat damage.)"

    keywords(Keyword.DOUBLE_STRIKE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Marco Nelor"
        flavorText = "Accustomed to battling before an audience in the arena, Iroas's champions know how to put on a good show."
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0441583-c9d5-47a1-8754-c9162cec64bc.jpg?1783938314"
    }
}
