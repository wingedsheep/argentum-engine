package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Duskshell Crawler — Modern Horizons 2 #156
 * {1}{G} · Creature — Insect · 0 / 3
 *
 * When this creature enters, put a +1/+1 counter on target creature.
 * Each creature you control with a +1/+1 counter on it has trample.
 *
 * The trample grant is a [GrantKeyword] static over
 * `Creature.youControl().withCounter(+1/+1)` (the same shape as Gnarlid Colony), so it includes
 * the Crawler itself once it carries a counter.
 */
val DuskshellCrawler = card("Duskshell Crawler") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    power = 0
    toughness = 3
    oracleText = "When this creature enters, put a +1/+1 counter on target creature.\n" +
        "Each creature you control with a +1/+1 counter on it has trample. (It can deal excess " +
        "combat damage to the player or planeswalker it's attacking.)"

    // When this creature enters, put a +1/+1 counter on target creature.
    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    // Each creature you control with a +1/+1 counter on it has trample.
    staticAbility {
        ability = GrantKeyword(
            Keyword.TRAMPLE,
            filter = GroupFilter(
                GameObjectFilter.Creature.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "Yeong-Hao Han"
        flavorText = "They often perch on street lamps, causing the poles to buckle under their weight."
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1af03fc3-5eb6-404e-96d9-e291a1e11ec3.jpg?1783926831"
    }
}
