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
 * Lightwalker — Dragons of Tarkir #24 (canonical printing)
 * {1}{W} · Creature — Human Warrior · 2/1
 *
 * This creature has flying as long as it has a +1/+1 counter on it.
 *
 * Sigiled Contender's shape: a self-scoped keyword grant gated on a +1/+1 counter.
 */
val Lightwalker = card("Lightwalker") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 1
    oracleText = "This creature has flying as long as it has a +1/+1 counter on it."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, Filters.Self),
            condition = Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Winona Nelson"
        flavorText = "\"The greatest gift Dromoka gives is the ability to fly without wings.\"\n—Urdnan, Dromoka warrior"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01bf1837-41b0-4ff1-9cb1-ee2d75d410c7.jpg?1783938616"
    }
}
