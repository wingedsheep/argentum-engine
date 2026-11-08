package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Ainok Artillerist — Dragons of Tarkir #171 (canonical printing)
 * {2}{G} · Creature — Dog Archer · 4/1
 *
 * This creature has reach as long as it has a +1/+1 counter on it.
 *
 * Lightwalker's shape: a self-scoped keyword grant gated on a +1/+1 counter.
 */
val AinokArtillerist = card("Ainok Artillerist") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dog Archer"
    power = 4
    toughness = 1
    oracleText = "This creature has reach as long as it has a +1/+1 counter on it. (It can block creatures with flying.)"

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.REACH, Filters.Self),
            condition = Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "171"
        artist = "James Paick"
        flavorText = "\"In the Tarkir I once knew, the Abzan revered their ancestor trees. Now, as Dromoka, they cut their finest groves to make weapons.\"\n—Sarkhan Vol"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a4c8964-06e4-4a24-9a7e-9cac0fb8518e.jpg?1783938582"
    }
}
