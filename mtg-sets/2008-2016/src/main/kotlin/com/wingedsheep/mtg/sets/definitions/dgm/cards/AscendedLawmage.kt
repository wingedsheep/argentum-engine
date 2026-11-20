package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ascended Lawmage
 * {2}{W}{U}
 * Creature — Vedalken Wizard
 * 3/2
 * Flying
 * Hexproof (This creature can't be the target of spells or abilities your opponents control.)
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val AscendedLawmage = card("Ascended Lawmage") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Vedalken Wizard"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "Hexproof (This creature can't be the target of spells or abilities your opponents control.)"

    keywords(Keyword.FLYING, Keyword.HEXPROOF)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Ryan Yee"
        flavorText = "A lawmage's runic script is an act of governance given form: legislation written directly onto the air itself."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1f00799-80ce-431e-97bb-8bb4e0e8ba49.jpg"
    }
}
