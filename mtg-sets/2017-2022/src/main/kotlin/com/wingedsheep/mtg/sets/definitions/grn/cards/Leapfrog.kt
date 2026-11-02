package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Leapfrog
 * {2}{U}
 * Creature — Frog
 * 3/1
 * This creature has flying as long as you've cast an instant or sorcery spell this turn.
 */
val Leapfrog = card("Leapfrog") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Frog"
    oracleText = "This creature has flying as long as you've cast an instant or sorcery spell this turn."
    power = 3
    toughness = 1

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, Filters.Self),
            condition = Conditions.YouCastSpellsThisTurn(1, GameObjectFilter.InstantOrSorcery),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42"
        artist = "Aaron Miller"
        flavorText = "\"Most compete for insects at street level. Some dwell near Izzet laboratories " +
            "and ride the thermal updrafts.\"\n—Yolov, Simic bioengineer"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e415a03-f7f8-4fca-954a-ef21b4a2c60f.jpg?1783934187"
    }
}
