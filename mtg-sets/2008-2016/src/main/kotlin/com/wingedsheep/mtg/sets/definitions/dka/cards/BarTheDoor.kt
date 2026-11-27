package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bar the Door
 * {2}{W}
 * Instant
 * Creatures you control get +0/+4 until end of turn.
 */
val BarTheDoor = card("Bar the Door") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Creatures you control get +0/+4 until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(0, 4, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Ryan Pancoast"
        flavorText = "\"It may not look like much, but it's a good old door with strong wards. It'll hold.\"\n—Olgard of the Skiltfolk"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b593f544-2d82-4237-b9a9-88503b5036cc.jpg"
    }
}
