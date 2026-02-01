package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Glory Seeker
 * {1}{W}
 * Creature — Human Soldier
 * 2/2
 */
val GlorySeeker = card("Glory Seeker") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Matt Cavotta"
        flavorText = "\"There's no contract to sign, no oath to swear. The enlistment procedure is to unsheathe your sword and point it at the enemy.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84861b25-7ae8-4350-9b70-e05d30965548.jpg"
    }
}
