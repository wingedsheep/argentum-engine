package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Absorb
 * {W}{U}{U}
 * Instant
 * Counter target spell. You gain 3 life.
 */
val Absorb = card("Absorb") {
    manaCost = "{W}{U}{U}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "Counter target spell. You gain 3 life."

    spell {
        target = Targets.Spell
        effect = Effects.CounterSpell() then Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Andrew Goldhawk"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d6a0f3e-457f-41f5-be26-5fb249874f1a.jpg?1562913952"
    }
}
