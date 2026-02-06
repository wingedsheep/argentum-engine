package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gluttonous Zombie
 * {4}{B}
 * Creature — Zombie
 * 3/3
 * Fear (This creature can't be blocked except by artifact creatures and/or black creatures.)
 */
val GluttonousZombie = card("Gluttonous Zombie") {
    manaCost = "{4}{B}"
    typeLine = "Creature — Zombie"
    power = 3
    toughness = 3

    keywords(Keyword.FEAR)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "151"
        artist = "Carl Critchlow"
        flavorText = "It knows nothing of allegiance or honor. It knows only hunger."
        imageUri = "https://cards.scryfall.io/large/front/d/b/db909e95-7979-41f0-b17a-874c4137fcc1.jpg?1562947268"
    }
}
