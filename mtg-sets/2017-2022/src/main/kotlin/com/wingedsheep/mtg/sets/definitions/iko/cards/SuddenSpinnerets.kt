package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sudden Spinnerets
 * {G}
 * Instant
 *
 * Target creature gets +1/+3 until end of turn. Put a reach counter on it. Untap it.
 *
 * The reach counter is a keyword counter ([CounterType.REACH]) — it outlasts the turn, unlike the
 * +1/+3.
 */
val SuddenSpinnerets = card("Sudden Spinnerets") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+3 until end of turn. Put a reach counter on it. Untap it."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 3, creature) then
            Effects.AddCounters(CounterType.REACH, 1, creature) then
            Effects.Untap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "171"
        artist = "Nicholas Gregory"
        flavorText = "\"What's natural on Ikoria continues to delight me.\"\n—Vivien Reid"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/3730996a-96d2-4311-a4dc-0045a2f8ccc9.jpg"
    }
}
