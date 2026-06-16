package com.wingedsheep.mtg.sets.definitions.tsp.cards

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
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell."

    spell {
        target = Targets.Spell
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Mark Poole"
        flavorText = "Fendros gasped as he watched the spell drip from the ends of his fingers. He moved his foot, afraid to disturb the spot where it lay slain."
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4e175f7-f649-451b-9ee5-ad1140b2e8a7.jpg"
    }
}
