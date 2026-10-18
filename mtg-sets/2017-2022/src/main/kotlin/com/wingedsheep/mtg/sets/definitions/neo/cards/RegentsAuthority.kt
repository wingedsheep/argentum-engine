package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Regent's Authority — Kamigawa: Neon Dynasty #32 (canonical printing)
 * {W} · Instant
 *
 * Target creature gets +2/+2 until end of turn. If it's an enchantment creature or legendary creature,
 * instead put a +1/+1 counter on it and it gets +1/+1 until end of turn.
 *
 * The enchantment/legendary check is made on resolution against the target's current (projected)
 * characteristics; the target is already a creature, so the filter only needs the enchantment or
 * legendary half. A creature that is both still gets just one counter and +1/+1 (2022-02-18 ruling).
 */
val RegentsAuthority = card("Regent's Authority") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. If it's an enchantment creature or " +
        "legendary creature, instead put a +1/+1 counter on it and it gets +1/+1 until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Enchantment or GameObjectFilter.Any.legendary(),
                t,
            ),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t) then Effects.ModifyStats(1, 1, t),
            otherwise = Effects.ModifyStats(2, 2, t),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "A. M. Sartor"
        flavorText = "Before her spark pulled her away again, the Emperor appointed the wise Light-Paws to " +
            "guide Kamigawa in her absence."
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79243e97-fe45-4988-b299-f59a7c53ad31.jpg?1783923916"
        ruling(
            "2022-02-18",
            "If it is both an enchantment creature and a legendary creature, you still only put one +1/+1 " +
                "counter on it and it gets +1/+1 until end of turn."
        )
    }
}
