package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Great Defender
 * {W}
 * Instant
 *
 * Target creature gets +0/+X until end of turn, where X is its mana value.
 *
 * X reads the *target's* mana value at resolution (same wiring as Subdue and Kry Shield).
 */
val GreatDefender = card("Great Defender") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +0/+X until end of turn, where X is its mana value."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(DynamicAmounts.fixed(0), DynamicAmounts.manaValueOf(creature), creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/879a8653-1538-4f78-a3d3-a900a4d9499b.jpg?1783948085"
    }
}
