package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Join Forces
 * {2}{W}
 * Instant
 * Untap up to two target creatures. They each get +2/+2 until end of turn.
 *
 * "Up to two target creatures" is one requirement taking up to two distinct objects; both the
 * untap and the pump apply once per chosen creature via ForEachTarget.
 */
val JoinForces = card("Join Forces") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Untap up to two target creatures. They each get +2/+2 until end of turn."

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Untap(EffectTarget.ContextTarget(0)),
            Effects.ModifyStats(2, 2, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Aurore Folny"
        flavorText = "\"Okay—you take the sixteen ragers and the hulk on the right. I'll take the ten defilers and the plaguelord on the left. Ready?\"\n—Danitha, to Radha"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4dce5e7-c864-4239-bab7-af6a3adcb731.jpg?1783921364"
    }
}
