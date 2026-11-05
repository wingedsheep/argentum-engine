package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blur of Blades
 * {1}{R}
 * Instant
 * Put a -1/-1 counter on target creature. Blur of Blades deals 2 damage to that creature's controller.
 *
 * State-based actions aren't checked until the spell finishes resolving, so a creature the counter
 * kills is still on the battlefield (with its controller intact) when the damage is dealt.
 */
val BlurOfBlades = card("Blur of Blades") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Put a -1/-1 counter on target creature. Blur of Blades deals 2 damage to that creature's controller."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, t) then
            Effects.DealDamage(2, EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Anna Steinbauer"
        flavorText = "Samut battled her way through the hordes, shouting the names of those who'd died in her crop and those she hoped yet to save."
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b53f5b9b-d24b-4e9a-bc90-7ed198cd1132.jpg?1783936033"
        ruling(
            "2017-07-14",
            "If the targeted creature is an illegal target by the time Blur of Blades resolves, it won't resolve and none of its effects will happen. Blur of Blades won't deal damage to any player."
        )
    }
}
