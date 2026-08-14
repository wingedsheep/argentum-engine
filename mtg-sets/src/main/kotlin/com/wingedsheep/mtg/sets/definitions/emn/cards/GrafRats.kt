package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Graf Rats
 * {1}{B}
 * Creature — Rat
 * 2/1
 *
 * Meld is not yet supported by the engine. As with Midnight Scavengers and the other
 * Eldritch Moon meld cards, the printed meld trigger remains in [oracleText] but is not wired.
 */
val GrafRats = card("Graf Rats") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat"
    oracleText = "At the beginning of combat on your turn, if you both own and control this " +
        "creature and a creature named Midnight Scavengers, exile them, then meld them into " +
        "Chittering Host."
    power = 2
    toughness = 1

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3dedaff6-bd69-4fe3-a301-f7ea7c2f2861.jpg?1783937482"
    }
}
