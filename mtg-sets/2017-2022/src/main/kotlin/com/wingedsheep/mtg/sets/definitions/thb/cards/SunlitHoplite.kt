package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sunlit Hoplite
 * {1}{W}
 * Creature — Human Soldier
 * 2/1
 * During your turn, this creature has first strike.
 * This creature gets +1/+0 as long as you control an Elspeth planeswalker.
 *
 * Leonin Surveyor's `IsYourTurn`-gated first strike plus Court Cleric's planeswalker-subtype
 * gate (Arisen Gorgon's `YouControl(Planeswalker.withSubtype(...))`), both recomputed at
 * projection.
 */
val SunlitHoplite = card("Sunlit Hoplite") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = "During your turn, this creature has first strike.\n" +
        "This creature gets +1/+0 as long as you control an Elspeth planeswalker."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter.source()),
            condition = Conditions.IsYourTurn,
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 0, GroupFilter.source()),
            condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Elspeth")),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "273"
        artist = "Tomasz Jedruszek"
        flavorText = "\"Even in her absence, the light Elspeth kindled in our hearts never faltered.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f89cf0f-f682-44f8-879c-ed1e22f849b0.jpg?1783931500"
    }
}
