package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Reckless Brute
 * {2}{R}
 * Creature — Ogre Warrior
 * 3/1
 * Haste
 * This creature attacks each combat if able.
 */
val RecklessBrute = card("Reckless Brute") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Warrior"
    oracleText = "Haste (This creature can attack and {T} as soon as it comes under your control.)\n" +
        "This creature attacks each combat if able."
    power = 3
    toughness = 1

    keywords(Keyword.HASTE)

    staticAbility { ability = MustAttack() }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Johann Bodin"
        flavorText = "His mind knows no fear, registering only the burning impulse to destroy."
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fd32a9e-1d39-4792-9657-69d17e5e0134.jpg"
    }
}
