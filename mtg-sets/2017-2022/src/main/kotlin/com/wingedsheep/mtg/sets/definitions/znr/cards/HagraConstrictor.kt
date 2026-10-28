package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GrantKeywordByCounter

/**
 * Hagra Constrictor — Zendikar Rising #105
 * {2}{B} · Creature — Snake · 0/0
 *
 * This creature enters with two +1/+1 counters on it.
 * Each creature you control with a +1/+1 counter on it has menace.
 */
val HagraConstrictor = card("Hagra Constrictor") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Snake"
    power = 0
    toughness = 0
    oracleText = "This creature enters with two +1/+1 counters on it.\n" +
        "Each creature you control with a +1/+1 counter on it has menace. " +
        "(A creature with menace can't be blocked except by two or more creatures.)"

    replacementEffect(
        EntersWithCounters(
            count = 2,
            selfOnly = true
        )
    )

    staticAbility {
        ability = GrantKeywordByCounter(Keyword.MENACE, CounterType.PLUS_ONE_PLUS_ONE, controllerOnly = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f971b71a-af69-43a7-b06c-e80eaaaf4a5f.jpg?1783929375"
        ruling("2020-09-25", "Multiple instances of menace on the same creature are redundant.")
    }
}
