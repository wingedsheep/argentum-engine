package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unleash
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Chaos Imps — Return to Ravnica #90 (canonical printing)
 * {4}{R}{R} · Creature — Imp · 6/5
 *
 * Flying
 * Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as
 * it has a +1/+1 counter on it.)
 * This creature has trample as long as it has a +1/+1 counter on it.
 */
val ChaosImps = card("Chaos Imps") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Imp"
    power = 6
    toughness = 5
    oracleText = "Flying\nUnleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as it has a +1/+1 counter on it.)\nThis creature has trample as long as it has a +1/+1 counter on it."

    keywords(Keyword.FLYING)
    unleash()

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.TRAMPLE, Filters.Self),
            condition = Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c70a702a-c9be-4bee-9087-22b5905f783a.jpg?1783940357"
    }
}
