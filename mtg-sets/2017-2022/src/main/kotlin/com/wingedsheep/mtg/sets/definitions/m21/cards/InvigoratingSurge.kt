package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Invigorating Surge — Core Set 2021 #190 (canonical printing)
 * {2}{G} · Instant
 *
 * Put a +1/+1 counter on target creature you control, then double the number of +1/+1 counters
 * on that creature.
 *
 * The add lands first, so [Effects.DoubleCounters] reads the new count and puts that many more
 * on — an ordinary placement that counter-placement replacements see, per the ruling.
 */
val InvigoratingSurge = card("Invigorating Surge") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature you control, then double the number of +1/+1 " +
        "counters on that creature."

    spell {
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t) then
            Effects.DoubleCounters(CounterType.PLUS_ONE_PLUS_ONE, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Randy Vargas"
        flavorText = "For hydras, most problems can be solved with the addition of a few extra heads."
        imageUri = "https://cards.scryfall.io/normal/front/5/3/531e1fc0-a3aa-4b57-87b2-79a31af5c922.jpg?1783930672"
        ruling("2020-06-23", "To double the number of +1/+1 counters on a creature, put a number of +1/+1 counters on it equal to the number it already has. Other cards that interact with putting counters on it will interact with this effect accordingly.")
    }
}
