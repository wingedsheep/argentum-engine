package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rally to Battle — Ravnica Allegiance #18
 * {3}{W} · Instant
 */
val RallyToBattle = card("Rally to Battle") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Creatures you control get +1/+3 until end of turn. Untap them."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.ModifyStats(1, 3, EffectTarget.IterationEntity)
        ) then
            Effects.ForEachInGroup(
                GroupFilter.AllCreaturesYouControl,
                Effects.Untap(EffectTarget.IterationEntity)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "18"
        artist = "Ben Wootten"
        flavorText = "The guildmasters loom large in the political sphere, but in the end, the rulers of Ravnica are its people."
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0798546-1ef4-485e-b094-cba12a12c67c.jpg?1783933720"
    }
}
