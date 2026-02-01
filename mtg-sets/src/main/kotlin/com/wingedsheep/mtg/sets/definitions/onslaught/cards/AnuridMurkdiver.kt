package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Anurid Murkdiver
 * {4}{B}{B}
 * Creature — Zombie Frog Beast
 * 4/3
 * Swampwalk (This creature can't be blocked as long as defending player controls a Swamp.)
 */
val AnuridMurkdiver = card("Anurid Murkdiver") {
    manaCost = "{4}{B}{B}"
    typeLine = "Creature — Zombie Frog Beast"
    power = 4
    toughness = 3

    keywords(Keyword.SWAMPWALK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Mark Tedin"
        flavorText = "The swamps ran deep beneath Otaria, and with them the frog's hunting grounds."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e43d62c-488a-4c8d-b193-bacbf8037761.jpg"
    }
}
