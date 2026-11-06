package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Precise Strike
 * {R}
 * Instant
 * Target creature gets +1/+0 and gains first strike until end of turn.
 */
val PreciseStrike = card("Precise Strike") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+0 and gains first strike until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 0, creature) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Tyler Jacobson"
        flavorText = "\"Hit where it hurts, and always hit first.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3cd2e7ab-b63e-48e8-a32a-6ff8673241d9.jpg"
    }
}
