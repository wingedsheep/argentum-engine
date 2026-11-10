package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Baloth Pup — Oath of the Gatewatch #127 (canonical printing)
 * {1}{G} · Creature — Beast · 3/1
 *
 * This creature has trample as long as it has a +1/+1 counter on it.
 *
 * Lightwalker's shape: a self-scoped keyword grant gated on a +1/+1 counter.
 */
val BalothPup = card("Baloth Pup") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 1
    oracleText = "This creature has trample as long as it has a +1/+1 counter on it."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.TRAMPLE, Filters.Self),
            condition = Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Steve Prescott"
        flavorText = "A young baloth channels its energy into playful destructiveness. Over time, it becomes " +
            "less playful but no less destructive."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f9c87f4-4fa5-4c97-9654-c4acd250f850.jpg?1783937902"
    }
}
