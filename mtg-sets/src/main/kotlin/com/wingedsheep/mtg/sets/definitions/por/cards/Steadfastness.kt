package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.dsl.Effects

/**
 * Steadfastness
 * {1}{W}
 * Sorcery
 * Creatures you control get +0/+3 until end of turn.
 */
val Steadfastness = card("Steadfastness") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            ModifyStatsEffect(0, 3, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Kev Walker"
        flavorText = "\"Brute force wins the battles. Conviction wins the wars.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb4693b3-d5d7-4401-aee3-119d3eb276a2.jpg"
    }
}
