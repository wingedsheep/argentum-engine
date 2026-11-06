package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shed Weakness
 * {G}
 * Instant
 * Target creature gets +2/+2 until end of turn. You may remove a -1/-1 counter from it.
 */
val ShedWeakness = card("Shed Weakness") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. You may remove a -1/-1 counter from it."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, t) then
            Effects.May(Effects.RemoveCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, t))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Christine Choi"
        flavorText = "To prepare for the Trial of Strength, nagas shed their old skin—and with it their scars, their doubts, and their fears."
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81803f44-6260-4df9-b566-2a2694d4deb0.jpg?1783936469"
    }
}
