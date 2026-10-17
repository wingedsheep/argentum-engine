package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Backstreet Bruiser
 * {1}{U}
 * Creature — Octopus Rogue
 * 3/3
 * Defender
 * As long as there are two or more counters among creatures you control, this creature can
 * attack as though it didn't have defender.
 */
val BackstreetBruiser = card("Backstreet Bruiser") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Octopus Rogue"
    power = 3
    toughness = 3
    oracleText = "Defender\nAs long as there are two or more counters among creatures you control, " +
        "this creature can attack as though it didn't have defender."

    keywords(Keyword.DEFENDER)

    // Counts individual counters of any kind, on one or several creatures (see ruling).
    staticAbility {
        ability = CanAttackDespiteDefender(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).totalCounters(),
                ComparisonOperator.GTE,
                2
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Aaron Miller"
        flavorText = "\"I could show you what's in the case, but it'd be the last thing you ever see.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d3a94c8-a7eb-4f05-8d48-d42e5129b09a.jpg?1783923150"

        ruling(
            "2022-04-29",
            "Backstreet Bruiser's ability looks for two or more individual counters. It doesn't matter if the " +
                "counters are the same kind or different kinds or whether they are on the same permanent or " +
                "different permanents."
        )
    }
}
