package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crashing Drawbridge
 * {2}
 * Artifact Creature — Wall
 * 0/4
 *
 * Defender
 * {T}: Creatures you control gain haste until end of turn.
 */
val CrashingDrawbridge = card("Crashing Drawbridge") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Wall"
    oracleText = "Defender\n" +
        "{T}: Creatures you control gain haste until end of turn."
    power = 0
    toughness = 4

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "217"
        artist = "Mark Behm"
        flavorText = "\"The Northern Beacon flared, and even before the drawbridge finished its descent, the knights charged out.\"\n—*Legend of the Gilded Knights*"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7d7108f-635c-423b-988a-bc8fc4c6edef.jpg?1783932587"
    }
}
