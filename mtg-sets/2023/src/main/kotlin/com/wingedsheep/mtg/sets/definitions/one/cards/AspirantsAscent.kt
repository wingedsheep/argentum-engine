package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Aspirant's Ascent
 * {U}
 * Instant
 * Until end of turn, target creature gets +1/+3 and gains flying and toxic 1.
 */
val AspirantsAscent = card("Aspirant's Ascent") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gets +1/+3 and gains flying and toxic 1. " +
        "(Players dealt combat damage by that creature also get a poison counter.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 3, t) then
            Effects.GrantKeyword(Keyword.FLYING, t) then
            Effects.GrantToxic(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Eli Minaya"
        flavorText = "For the aspirants of the Progress Engine, being launched into the sphere's upper reaches is a great honor. Some are even granted wings beforehand."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57551332-e2a4-4e6a-9bd8-e3a9baafcd17.jpg?1783918070"
    }
}
