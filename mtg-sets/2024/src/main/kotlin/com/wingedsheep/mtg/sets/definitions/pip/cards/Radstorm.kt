package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Radstorm
 * {3}{U}
 * Instant
 *
 * Storm (When you cast this spell, copy it for each spell cast before it this turn.)
 * Proliferate.
 *
 * Storm copies the spell off `script.spellEffect`, so each copy proliferates on its own.
 */
val Radstorm = card("Radstorm") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Storm (When you cast this spell, copy it for each spell cast before it this turn.)\n" +
        "Proliferate. (Choose any number of permanents and/or players, then give each another counter " +
        "of each kind already there.)"

    spell {
        effect = Effects.Proliferate()
    }

    keywords(Keyword.STORM)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Salvatorre Zee Yazzie"
        flavorText = "Green sky at night, ghoul's delight."
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d778cdec-8fc7-4174-bae1-4c8e8ccdfab3.jpg?1783912412"
    }
}
