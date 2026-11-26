package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Harbor Serpent
 * {4}{U}{U}
 * Creature — Serpent
 * 5/5
 * Islandwalk
 * This creature can't attack unless there are five or more Islands on the battlefield.
 *
 * "On the battlefield" counts every player's Islands (lands with the Island subtype, not only
 * ones named Island), so it's a `battlefield(Player.Each)` count.
 */
val HarborSerpent = card("Harbor Serpent") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Serpent"
    power = 5
    toughness = 5
    oracleText = "Islandwalk (This creature can't be blocked as long as defending player controls an Island.)\n" +
        "This creature can't attack unless there are five or more Islands on the battlefield."

    keywords(Keyword.ISLANDWALK)

    staticAbility {
        ability = CantAttackUnless(
            Conditions.CompareAmounts(
                DynamicAmounts.battlefield(
                    Player.Each,
                    GameObjectFilter.Land.withSubtype(Subtype.ISLAND),
                ).count(),
                ComparisonOperator.GTE,
                5,
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Daarken"
        flavorText = "Like most giant monsters villagers worship as gods, it proves a fickle master."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa10b43f-eb63-4999-92a0-56826031b686.jpg?1783941826"
        ruling("2010-08-15", "Harbor Serpent's abilities care about lands with the land type Island, not necessarily lands named Island.")
        ruling("2010-08-15", "The second ability checks how many Islands are on the battlefield (regardless of who controls them) only as attackers are declared. Once Harbor Serpent is declared as an attacker, it will continue to attack even if the number of Islands on the battlefield falls below five.")
    }
}
