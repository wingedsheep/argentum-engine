package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rallying Roar
 * {2}{W}
 * Instant
 * Creatures you control get +1/+1 until end of turn. Untap them.
 */
val RallyingRoar = card("Rallying Roar") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Creatures you control get +1/+1 until end of turn. Untap them."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity) then
                Effects.Untap(EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "John Stanko"
        flavorText = "\"The sun's strength sings in our hearts! I have never been more proud of my home and my people.\"\n—Huatli"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d03a5efb-fde9-4bba-be80-d073eba2ad8d.jpg?1783935794"
        ruling("2017-09-29", "Rallying Roar affects only creatures you control at the time it resolves. Creatures you begin to control later in the turn won't get +1/+1.")
        ruling("2017-09-29", "Untapped creatures you control get +1/+1 even though Rallying Roar doesn't untap them.")
    }
}
