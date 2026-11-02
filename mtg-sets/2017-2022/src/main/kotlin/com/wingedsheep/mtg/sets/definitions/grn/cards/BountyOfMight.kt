package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bounty of Might
 * {4}{G}{G}
 * Instant
 * Target creature gets +3/+3 until end of turn.
 * Target creature gets +3/+3 until end of turn.
 * Target creature gets +3/+3 until end of turn.
 *
 * Each "target" is its own instance of the word, so the same creature may be chosen for any or all
 * of them (cross-requirement duplicates are legal; see TargetDistinctnessScenarioTest).
 */
val BountyOfMight = card("Bounty of Might") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +3/+3 until end of turn.\n" +
        "Target creature gets +3/+3 until end of turn.\n" +
        "Target creature gets +3/+3 until end of turn."

    spell {
        val first = target(TargetFilter.Creature)
        val second = target(TargetFilter.Creature)
        val third = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(3, 3, first) then
            Effects.ModifyStats(3, 3, second) then
            Effects.ModifyStats(3, 3, third)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Even Amundsen"
        flavorText = "\"I am the very soul of battle, but even I would never advise open war with the Conclave.\"\n—Aurelia"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9a950d8-e6e2-42d6-83d7-a60c15d2035d.jpg?1783934154"
    }
}
