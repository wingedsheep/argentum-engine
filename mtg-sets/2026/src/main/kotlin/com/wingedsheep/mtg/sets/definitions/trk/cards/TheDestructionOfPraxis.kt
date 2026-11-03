package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * The Destruction of Praxis
 * {3}{B}{B}
 * Sorcery
 *
 * Strange new worlds — All creatures get -X/-X until end of turn, where X is the number of differently
 * named lands you control. If X is 6 or more, create two 2/1 red Klingon Warrior creature tokens with haste.
 *
 * "Strange new worlds" is an ability word with no rules meaning. X is counted on resolution and the
 * stat change locks that value in for the turn. The -X/-X can't change the land count, so the "6 or more"
 * check reads the same X; the tokens are created after the shrink, so they aren't affected by it.
 */
val TheDestructionOfPraxis = card("The Destruction of Praxis") {
    manaCost = "{3}{B}{B}"
    typeLine = "Sorcery"
    colorIdentity = "B"
    oracleText = "Strange new worlds — All creatures get -X/-X until end of turn, where X is the number of " +
        "differently named lands you control. If X is 6 or more, create two 2/1 red Klingon Warrior " +
        "creature tokens with haste."

    spell {
        val x = DynamicAmounts.differentlyNamedLandsYouControl()
        effect = Patterns.Group.modifyStatsForAll(-x, -x, GroupFilter.AllCreatures) then
            Effects.If(
                condition = Conditions.CompareAmounts(x, ComparisonOperator.GTE, 6),
                then = Effects.CreateToken(
                    power = 2, toughness = 1, colors = setOf(Color.RED),
                    creatureTypes = setOf("Klingon", "Warrior"), keywords = setOf(Keyword.HASTE),
                    count = 2,
                ),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Pace Wilder"
        flavorText = "Centuries of unregulated dilithium mining left the only moon of Qo'noS a ticking time bomb."
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d33ac2a-73a9-4804-bf9f-c8cf4eea1c76.jpg?1793187997"
    }
}
