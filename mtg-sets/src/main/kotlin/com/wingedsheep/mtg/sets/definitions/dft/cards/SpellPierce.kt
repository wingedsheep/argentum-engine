package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spell Pierce
 * {U}
 * Instant
 *
 * Counter target noncreature spell unless its controller pays {2}.
 */
val SpellPierce = card("Spell Pierce") {
    manaCost = "{U}"
    typeLine = "Instant"
    oracleText = "Counter target noncreature spell unless its controller pays {2}."

    spell {
        target = Targets.NoncreatureSpell
        effect = Effects.CounterUnlessPays("{2}")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Maxime Minard"
        flavorText = "The druid's aim was dead on. But their choice of target was dead wrong."
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8dd4374f-0301-4b2e-bc99-2cd19568cb3b.jpg?1738359138"
    }
}
