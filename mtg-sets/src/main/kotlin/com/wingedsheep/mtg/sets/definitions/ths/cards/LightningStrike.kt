package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lightning Strike
 * {1}{R}
 * Instant
 * Lightning Strike deals 3 damage to any target.
 *
 * Canonical printing: Theros (THS, 2013) — the earliest real-expansion printing.
 * Later printings (incl. Avatar: The Last Airbender) add only a [com.wingedsheep.sdk.model.Printing] row.
 */
val LightningStrike = card("Lightning Strike") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Lightning Strike deals 3 damage to any target."

    spell {
        val t = target("any target", Targets.Any)
        effect = Effects.DealDamage(3, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "127"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbb03f2e-2b92-4aa1-afae-301ed5d151d3.jpg?1562827848"
    }
}
