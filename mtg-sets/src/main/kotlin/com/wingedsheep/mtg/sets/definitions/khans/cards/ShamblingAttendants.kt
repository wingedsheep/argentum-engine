package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Shambling Attendants
 * {7}{B}
 * Creature — Zombie
 * 3/5
 * Delve
 * Deathtouch
 */
val ShamblingAttendants = card("Shambling Attendants") {
    manaCost = "{7}{B}"
    typeLine = "Creature — Zombie"
    power = 3
    toughness = 5

    keywords(Keyword.DELVE, Keyword.DEATHTOUCH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Daarken"
        flavorText = "\"Let the world behold what becomes of those who defy us.\"\n—Taigam, Sidisi's Hand"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/459ca25c-775f-4110-8d70-59720692ab27.jpg?1562785839"
    }
}
