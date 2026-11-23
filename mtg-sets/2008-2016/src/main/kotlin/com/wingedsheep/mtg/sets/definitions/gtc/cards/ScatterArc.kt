package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scatter Arc
 * {3}{U}
 * Instant
 * Counter target noncreature spell.
 * Draw a card.
 */
val ScatterArc = card("Scatter Arc") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target noncreature spell.\nDraw a card."

    spell {
        target(TargetFilter.NoncreatureSpellOnStack)
        effect = Effects.CounterSpell() then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Peter Mohrbacher"
        flavorText = "\"Should your chosen emissary be able to fluctuate triharmonic pentodes, however unlikely, " +
            "I shall shunt excess negavolt radicals.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32ed969f-2c8e-4421-9448-dc5a2afdc81d.jpg?1783940135"
    }
}
