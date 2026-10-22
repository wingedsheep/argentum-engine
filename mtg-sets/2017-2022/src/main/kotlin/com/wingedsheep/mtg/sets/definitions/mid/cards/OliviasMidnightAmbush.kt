package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Olivia's Midnight Ambush (Innistrad: Midnight Hunt) — {1}{B} Instant
 *
 * "Target creature gets -2/-2 until end of turn. If it's night, that creature gets -13/-13 until end of
 *  turn instead."
 *
 * "Instead" is a resolution-time either/or: [Conditions.IsNight] (CR 731) picks the -13/-13 branch,
 * otherwise (day, or no day/night designation at all) the -2/-2 applies. Exactly one modification is
 * ever applied, so the two never stack.
 */
val OliviasMidnightAmbush: CardDefinition = card("Olivia's Midnight Ambush") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -2/-2 until end of turn. If it's night, that creature gets " +
        "-13/-13 until end of turn instead."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.IsNight,
            then = Effects.ModifyStats(-13, -13, creature),
            otherwise = Effects.ModifyStats(-2, -2, creature),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c1adede-22ad-4c1c-9501-ad731fbe1742.jpg?1783925610"
    }
}
