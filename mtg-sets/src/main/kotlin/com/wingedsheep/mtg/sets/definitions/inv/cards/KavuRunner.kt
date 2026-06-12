package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Kavu Runner
 * {3}{R}
 * Creature — Kavu
 * 3/3
 * This creature has haste as long as no opponent controls a white or blue creature.
 */
val KavuRunner = card("Kavu Runner") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Kavu"
    power = 3
    toughness = 3
    oracleText = "This creature has haste as long as no opponent controls a white or blue creature."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HASTE, Filters.Self),
            condition = Conditions.Not(
                Exists(
                    Player.EachOpponent,
                    Zone.BATTLEFIELD,
                    GameObjectFilter.Creature.withAnyColor(Color.WHITE, Color.BLUE)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "150"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bc1b462-4e3c-47cc-87c5-f6e29dd70c01.jpg?1562903915"
    }
}
