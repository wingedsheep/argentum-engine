package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Common Bond
 * {1}{G}{W}
 * Instant
 * Put a +1/+1 counter on target creature.
 * Put a +1/+1 counter on target creature.
 *
 * Canonical printing: Return to Ravnica, the card's only printing.
 *
 * Two independent "target creature" requirements (the Seeds of Strength shape), so the same
 * creature may be chosen for both and receive two counters (Scryfall ruling, 2012-10-01).
 */
val CommonBond = card("Common Bond") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature.\n" +
        "Put a +1/+1 counter on target creature."

    spell {
        val first = target(TargetFilter.Creature)
        val second = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, first) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, second)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "151"
        artist = "Raymond Swanland"
        flavorText = "Wolf riders hone their skills traversing the perilous rooftops, each dizzying " +
            "step bringing elf and wolf closer together."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59965953-1522-4103-9a6c-5534205d34d9.jpg?1783940343"
        ruling(
            "2012-10-01",
            "You may choose the same creature for both targets. You may also choose two different " +
                "creatures. This is because the word “target” is used twice.",
        )
    }
}
