package com.wingedsheep.mtg.sets.definitions.foundations.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Negate {1}{U}
 * Instant
 *
 * Counter target noncreature spell.
 */
val Negate = card("Negate") {
    manaCost = "{1}{U}"
    typeLine = "Instant"
    oracleText = "Counter target noncreature spell."

    spell {
        target = Targets.NoncreatureSpell
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "710"
        artist = "Magali Villeneuve"
        flavorText = "\"As one, nature lifts its voice to tell you this: 'No.'\""
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff3b0dba-0207-4249-bdab-e807c76ce39e.jpg?1730572717"
    }
}
