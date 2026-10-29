package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Sigiled Contender — Core Set 2021 #323 (canonical printing)
 * {3}{W} · Creature — Human Warrior · 3/3
 *
 * This creature has lifelink as long as it has a +1/+1 counter on it.
 *
 * Myojin of Cleansing Fire's shape with a +1/+1 counter as the gate. Filters.Self: GrantKeyword's
 * default filter is attached-scope (Aura/Equipment shape).
 */
val SigiledContender = card("Sigiled Contender") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 3
    oracleText = "This creature has lifelink as long as it has a +1/+1 counter on it. (Damage dealt by " +
        "this creature also causes you to gain that much life.)"

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.LIFELINK, Filters.Self),
            condition = Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "323"
        artist = "Randy Vargas"
        flavorText = "\"All right. Warm-ups are over.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5ba81e67-8fa5-441c-bf14-2d75c168a617.jpg?1783930622"
    }
}
