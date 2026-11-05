package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Magma Spray
 * {R}
 * Instant
 * Magma Spray deals 2 damage to target creature. If that creature would die this turn, exile it
 * instead.
 */
val MagmaSpray = card("Magma Spray") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Magma Spray deals 2 damage to target creature. If that creature would die this turn, " +
        "exile it instead."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(2, t) then Effects.MarkExileOnDeath(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "Jarreau Wimberly"
        flavorText = "\"Jund is a wounded world. Beware its searing blood spilling out onto the surface.\"\n—Rakka Mar"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/708ec768-f455-48e4-b834-e0a2b3768c45.jpg"
    }
}
