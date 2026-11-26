package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Duskmantle Prowler
 * {3}{B}
 * Creature — Vampire Rogue
 * 2/2
 * Haste
 * Exalted
 */
val DuskmantleProwler = card("Duskmantle Prowler") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    oracleText = "Haste (This creature can attack and {T} as soon as it comes under your control.)\n" +
        "Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn.)"
    power = 2
    toughness = 2

    keywords(Keyword.HASTE, Keyword.EXALTED)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcb031da-d41a-496a-b78e-0773f6504303.jpg"
    }
}
