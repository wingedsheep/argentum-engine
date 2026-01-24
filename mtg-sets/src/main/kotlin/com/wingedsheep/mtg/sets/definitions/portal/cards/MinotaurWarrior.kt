package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Minotaur Warrior
 * {2}{R}
 * Creature — Minotaur Warrior
 * 2/3
 */
val MinotaurWarrior = card("Minotaur Warrior") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Minotaur Warrior"
    power = 2
    toughness = 3

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Scott M. Fischer"
        flavorText = "The herd's patience, the stampede's fury."
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3ab1c2d3-e4f5-a6b7-c8d9-e0f1a2b3c4d5.jpg"
    }
}
