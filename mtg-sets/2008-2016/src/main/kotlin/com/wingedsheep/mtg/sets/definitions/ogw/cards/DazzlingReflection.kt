package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/** Dazzling Reflection — Oath of the Gatewatch #17. */
val DazzlingReflection = card("Dazzling Reflection") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "You gain life equal to target creature's power. " +
        "The next time that creature would deal damage this turn, prevent that damage."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainLife(DynamicAmounts.powerOf(creature)) then
            Effects.PreventDamage(
                target = creature,
                direction = PreventionDirection.FromTarget,
                nextInstanceOnly = true
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "David Palumbo"
        flavorText = "\"The light of a felidar's horns can give even an Eldrazi pause.\"\n—Zarra, lullmage"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f77977e8-25d8-4531-a6fc-8c46e6904670.jpg?1783937927"
    }
}
