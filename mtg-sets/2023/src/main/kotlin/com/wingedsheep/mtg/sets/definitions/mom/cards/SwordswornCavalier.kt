package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Swordsworn Cavalier
 * {1}{W}
 * Creature — Human Knight
 * 3/1
 *
 * This creature has first strike as long as another Knight entered the battlefield under your
 * control this turn.
 *
 * "Another Knight" is counted with the turn-tracked Knight-entry count, which also records this
 * creature's own entry. So the threshold is one Knight entry if the Cavalier didn't enter this turn,
 * and two if it did (its own entry plus another).
 */
val SwordswornCavalier = card("Swordsworn Cavalier") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 1
    oracleText = "This creature has first strike as long as another Knight entered the battlefield " +
        "under your control this turn."

    val knightsEntered = DynamicAmounts.subtypeEnteredUnderControlThisTurn(Subtype.KNIGHT)
    val anotherKnightEntered = Conditions.Any(
        Conditions.All(
            Conditions.Not(Conditions.SourceEnteredThisTurn),
            Conditions.CompareAmounts(knightsEntered, ComparisonOperator.GTE, 1),
        ),
        Conditions.CompareAmounts(knightsEntered, ComparisonOperator.GTE, 2),
    )

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter.source())
        condition = anotherKnightEntered
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42"
        artist = "Stella Spente"
        flavorText = "The Royal Knights of Belenon are equally practiced in courtesy and combat."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa0461fc-4298-45b9-90a0-939093cf2544.jpg?1783917046"
    }
}
