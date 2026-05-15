package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Flame Lash
 * {3}{R}
 * Instant
 *
 * Flame Lash deals 4 damage to any target.
 */
val FlameLash = card("Flame Lash") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Flame Lash deals 4 damage to any target."

    spell {
        val any = target("any", Targets.Any)
        effect = Effects.DealDamage(4, any)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "391"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6440439-7178-4a97-9e18-7fdef4b02678.jpg?1721428094"
        inBooster = false
    }
}
