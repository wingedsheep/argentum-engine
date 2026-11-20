package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Last Breath
 * {1}{W}
 * Instant
 * Exile target creature with power 2 or less. Its controller gains 4 life.
 *
 * The power cap is part of the targeting restriction, so it is re-checked on resolution: a target
 * pumped above 2 in response makes the spell fizzle (no exile, no life). The life gain is sequenced
 * before the exile so [EffectTarget.TargetController] is read while the creature is still on the
 * battlefield (Nature's Claim / Swords to Plowshares shape).
 */
val LastBreath = card("Last Breath") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature with power 2 or less. Its controller gains 4 life."

    spell {
        val creature = target(TargetFilter(GameObjectFilter.Creature.powerAtMost(2)))
        effect = Effects.GainLife(4, EffectTarget.TargetController) then
            Effects.Exile(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "DiTerlizzi"
        flavorText = "\"The River flows. My soul returns.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b540da2-f8c6-48d6-af6d-db78958f0a17.jpg?1783945979"
        ruling(
            "2013-09-15",
            "If the creature is an illegal target when Last Breath tries to resolve, it won't resolve " +
                "and none of its effects will happen. Its controller won't gain 4 life."
        )
    }
}
