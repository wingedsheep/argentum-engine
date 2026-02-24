package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DivideCombatDamageFreely

/**
 * Butcher Orgg
 * {4}{R}{R}{R}
 * Creature — Orgg
 * 6/6
 * You may assign Butcher Orgg's combat damage divided as you choose among
 * defending player and/or any number of creatures they control.
 */
val ButcherOrgg = card("Butcher Orgg") {
    manaCost = "{4}{R}{R}{R}"
    typeLine = "Creature — Orgg"
    power = 6
    toughness = 6
    oracleText = "You may assign Butcher Orgg's combat damage divided as you choose among defending player and/or any number of creatures they control."

    staticAbility {
        ability = DivideCombatDamageFreely()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "192"
        artist = "Kev Walker"
        flavorText = "It can kill you with three arms tied behind its back."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f2a29cf-4b2e-44c0-af73-512d6fed0dae.jpg?1562925005"
    }
}
