package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glorious Charge
 * {1}{W}
 * Instant
 * Creatures you control get +1/+1 until end of turn.
 */
val GloriousCharge = card("Glorious Charge") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Creatures you control get +1/+1 until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Izzy"
        flavorText = "\"One's blade is only as sharp as one's conviction.\"\n—Ajani Goldmane"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c6dfcf8-a09b-4402-af80-90fe15b2ce0a.jpg"
    }
}
