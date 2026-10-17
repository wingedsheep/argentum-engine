package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Rhox Pummeler
 * {5}{G}
 * Creature — Rhino Soldier
 * 6 / 3
 * This creature enters with a shield counter on it. (If it would be dealt damage or destroyed, remove a shield counter from it instead.)
 * This creature has trample as long as it has a shield counter on it.
 */
val RhoxPummeler = card("Rhox Pummeler") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Rhino Soldier"
    oracleText = "This creature enters with a shield counter on it. (If it would be dealt damage or destroyed, remove a shield counter from it instead.)\nThis creature has trample as long as it has a shield counter on it."
    power = 6
    toughness = 3

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.SHIELD,
            count = 1,
            selfOnly = true,
        )
    )

    // Filters.Self: GrantKeyword's default filter is attached-scope (Aura/Equipment shape).
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.TRAMPLE, Filters.Self),
            condition = Conditions.SourceHasCounter(CounterType.SHIELD),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "155"
        artist = "Gabor Szikszai"
        flavorText = "When all you have is a warhammer, every face looks like a nail."
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c99f5b02-16b9-4672-b0ec-f77662e9de76.jpg?1783923099"
        ruling("2022-04-29", "If an attacking Rhox Pummeler with one shield counter on it is blocked by a creature with first strike, the shield counter will be removed in the first combat damage step. In that case, Rhox Pummeler won't have trample when it deals combat damage in the second combat damage step.")
    }
}
