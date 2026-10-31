package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Turn into a Pumpkin
 * {3}{U}
 * Instant
 * Return target nonland permanent to its owner's hand. Draw a card.
 * Adamant — If at least three blue mana was spent to cast this spell, create a Food token.
 *
 * Adamant counts blue mana spent on the whole cost, generic included, read off this spell's own
 * cast payment via [DynamicAmounts.manaOfColorSpent] (see Silverflame Ritual).
 */
val TurnIntoAPumpkin = card("Turn into a Pumpkin") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target nonland permanent to its owner's hand. Draw a card.\n" +
        "Adamant — If at least three blue mana was spent to cast this spell, create a Food token. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.ReturnToHand(permanent) then Effects.DrawCards(1) then Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.manaOfColorSpent(Color.BLUE),
                ComparisonOperator.GTE,
                3,
            ),
            Effects.CreateFood(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2a37dcb-4a18-4f03-b28c-27188a1a5ec1.jpg?1783932647"
    }
}
