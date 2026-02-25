package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Whirlwind Adept
 * {4}{U}
 * Creature — Djinn Monk
 * 4/2
 * Hexproof
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 */
val WhirlwindAdept = card("Whirlwind Adept") {
    manaCost = "{4}{U}"
    typeLine = "Creature — Djinn Monk"
    power = 4
    toughness = 2
    oracleText = "Hexproof (This creature can't be the target of spells or abilities your opponents control.)\nProwess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)"

    keywords(Keyword.HEXPROOF)
    prowess()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Steve Argyle"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51c9bd9c-900a-48a1-9cfe-2f8d031af6b8.jpg?1562786541"
    }
}
