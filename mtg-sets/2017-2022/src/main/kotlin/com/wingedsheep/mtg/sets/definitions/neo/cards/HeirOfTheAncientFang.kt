package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Heir of the Ancient Fang — Kamigawa: Neon Dynasty #191 (canonical printing)
 * {2}{G} · Creature — Snake Samurai · 2/3
 *
 * This creature enters with a +1/+1 counter on it if you control a modified creature.
 *
 * A self-only [EntersWithCounters] replacement gated on "you control a modified creature"
 * ([StatePredicate.IsModified], CR 700.9) — the same shape as Ascendant Packleader. The check is
 * made as Heir enters, when it is not yet on the battlefield, so it never counts itself.
 */
val HeirOfTheAncientFang = card("Heir of the Ancient Fang") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Samurai"
    oracleText = "This creature enters with a +1/+1 counter on it if you control a modified creature. " +
        "(Equipment, Auras you control, and counters are modifications.)"
    power = 2
    toughness = 3

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            selfOnly = true,
            condition = Conditions.YouControl(
                GameObjectFilter.Creature.withStatePredicate(StatePredicate.IsModified)
            ),
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "191"
        artist = "Joseph Weston"
        flavorText = "On the last day of a Jukai agent's training, they are bequeathed a powerful relic " +
            "blessed by the kami."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d45daa56-6ad8-4df7-9d81-3193fd32e574.jpg?1783923848"
    }
}
