package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Cyclops Tyrant — Magic 2014 #135 (canonical printing)
 * {5}{R}
 * Creature — Cyclops
 * 3/4
 * Intimidate
 * This creature can't block creatures with power 2 or less.
 *
 * The block restriction is expressed as its complement (it can block only creatures with power 3
 * or greater), as on Zurgo Bellstriker and Orgg; the attacker's projected power is read when blocks
 * are declared, so shrinking the attacker afterwards doesn't undo a legal block.
 */
val CyclopsTyrant = card("Cyclops Tyrant") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Cyclops"
    power = 3
    toughness = 4
    oracleText = "Intimidate (This creature can't be blocked except by artifact creatures and/or creatures that share a color with it.)\n" +
        "This creature can't block creatures with power 2 or less."

    keywords(Keyword.INTIMIDATE)

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(GameObjectFilter.Creature.powerAtLeast(3))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Zack Stella"
        flavorText = "A single eye, blinded by rage."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0b8e733-22a7-4696-83b3-297cbe75dadc.jpg?1783939914"
        ruling("2013-07-01", "If Cyclops Tyrant blocks a creature with power 3 or greater, and then that creature's power becomes 2 or less, Cyclops Tyrant continues to block that creature.")
    }
}
