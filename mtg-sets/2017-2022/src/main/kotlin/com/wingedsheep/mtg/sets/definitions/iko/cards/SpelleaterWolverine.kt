package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Spelleater Wolverine
 * {2}{R}
 * Creature — Wolverine
 * 3/2
 * This creature has double strike as long as there are three or more instant and/or sorcery
 * cards in your graveyard.
 *
 * Ghitu Lavarunner's graveyard-count condition gating a self-granted keyword.
 */
val SpelleaterWolverine = card("Spelleater Wolverine") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wolverine"
    power = 3
    toughness = 2
    oracleText = "This creature has double strike as long as there are three or more instant and/or sorcery cards in your graveyard."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DOUBLE_STRIKE, GroupFilter.source()),
            condition = Conditions.CompareAmounts(
                DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery),
                ComparisonOperator.GTE,
                3
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Uriah Voth"
        flavorText = "\"The monster that scares me the most isn't the biggest.\"\n—Taelya, Lavabrink pyromage"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5f03ffd-dcdb-441c-8dfc-4fe06a289b22.jpg?1783931042"
    }
}
