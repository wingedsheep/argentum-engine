package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cancel
 * {1}{U}{U}
 * Instant
 * Counter target spell.
 */
val Cancel = card("Cancel") {
    manaCost = "{1}{U}{U}"
    typeLine = "Instant"
    oracleText = "Counter target spell."

    spell {
        target = Targets.Spell
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Slawomir Maniak"
        flavorText = "\"Even the greatest inferno begins as a spark. And anyone can snuff out a spark.\" —Chanyi, mistfire sage"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f540dcb-8d0b-4d33-8c0d-893fa5db54eb.jpg?1562791164"
    }
}
