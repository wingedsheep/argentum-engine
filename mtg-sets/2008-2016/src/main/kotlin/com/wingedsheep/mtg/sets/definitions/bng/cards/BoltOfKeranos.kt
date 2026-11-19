package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bolt of Keranos
 * {1}{R}{R}
 * Sorcery
 *
 * Bolt of Keranos deals 3 damage to any target. Scry 1.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val BoltOfKeranos = card("Bolt of Keranos") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Bolt of Keranos deals 3 damage to any target. Scry 1. (Look at the top card of your " +
        "library. You may put that card on the bottom.)"

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(3, t) then Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Karl Kopinski"
        flavorText = "The breakthroughs granted by Keranos, god of epiphany, can be surprisingly literal."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4df70b14-5d67-4a92-aaba-72480c621d10.jpg?1783939552"
    }
}
