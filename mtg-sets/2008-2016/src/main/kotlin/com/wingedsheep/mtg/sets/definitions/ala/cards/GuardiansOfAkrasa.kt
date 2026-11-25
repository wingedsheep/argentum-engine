package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Guardians of Akrasa — Shards of Alara #12 (canonical printing)
 * {2}{W} · Creature — Human Soldier · 0/4
 *
 * Defender
 * Exalted
 */
val GuardiansOfAkrasa = card("Guardians of Akrasa") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "Defender (This creature can't attack.)\nExalted (Whenever a creature you control attacks " +
        "alone, that creature gets +1/+1 until end of turn.)"
    power = 0
    toughness = 4

    keywords(Keyword.DEFENDER, Keyword.EXALTED)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "Alan Pollack"
        flavorText = "\"Only by the bravery of those who put loyalty above glory is our home kept safe.\"\n—Elspeth"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f718030-cc41-4e0d-a1ca-a33f577dc1fb.jpg?1783942582"
    }
}
