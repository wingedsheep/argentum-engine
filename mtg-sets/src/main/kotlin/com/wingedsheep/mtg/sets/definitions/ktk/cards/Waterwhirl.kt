package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Effects

/**
 * Waterwhirl
 * {4}{U}{U}
 * Instant
 * Return up to two target creatures to their owners' hands.
 */
val Waterwhirl = card("Waterwhirl") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return up to two target creatures to their owners' hands."

    spell {
        target = TargetObject(
            count = 2,
            optional = true,
            filter = TargetFilter.Creature
        )
        effect = ForEachTargetEffect(
            effects = listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "Clint Cearley"
        flavorText = "\"Be as water: untamed and unheld, yet inexorably flowing toward a greater goal.\"\n—Shensu, Riverwheel mystic"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9dd58503-d269-4756-a71c-a6a2bfb1658d.jpg?1562791100"
    }
}
