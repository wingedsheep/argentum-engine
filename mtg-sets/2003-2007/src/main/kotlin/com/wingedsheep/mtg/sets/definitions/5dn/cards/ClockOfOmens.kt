package com.wingedsheep.mtg.sets.definitions.`5dn`.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Clock of Omens
 * {4}
 * Artifact
 * Tap two untapped artifacts you control: Untap target artifact.
 *
 * Clock of Omens may tap itself as one of the two artifacts (the cost doesn't say "other").
 */
val ClockOfOmens = card("Clock of Omens") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "Tap two untapped artifacts you control: Untap target artifact."

    activatedAbility {
        cost = Costs.TapPermanents(2, GameObjectFilter.Artifact)
        val t = target(TargetFilter.Artifact)
        effect = Effects.Untap(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Alex Horley-Orlandelli"
        flavorText = "Those near the clock don't hear it anymore, but their every movement is in time with its ticking."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0ffce71b-eb60-4649-a62b-a1b4acaa9d2d.jpg?1783944385"
    }
}
