package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bloodcurdling Scream
 * {X}{B}
 * Sorcery
 * Target creature gets +X/+0 until end of turn.
 */
val BloodcurdlingScream = card("Bloodcurdling Scream") {
    manaCost = "{X}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target creature gets +X/+0 until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(DynamicAmounts.xValue(), DynamicAmounts.fixed(0), creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Dan Frazier"
        flavorText = "\"I have all the weapons my enemies have—and far deeper rage.\"\n—Tojira, swamp queen"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dcb0ec62-6d6f-4d10-bc2d-37f25495f884.jpg?1783946477"
    }
}
