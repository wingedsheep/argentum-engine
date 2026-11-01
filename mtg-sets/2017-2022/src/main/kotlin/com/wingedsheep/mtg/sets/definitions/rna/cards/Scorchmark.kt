package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scorchmark — Ravnica Allegiance #113
 * {1}{R} · Instant
 */
val Scorchmark = card("Scorchmark") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Scorchmark deals 2 damage to target creature. If that creature would die this turn, exile it instead."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(2, t) then Effects.MarkExileOnDeath(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Jason Felix"
        flavorText = "\"Have you seen your face? Trust me, this will be an improvement.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a14834f-78ae-4ca5-9ce0-5b928ad3d76c.jpg?1783933677"
    }
}
