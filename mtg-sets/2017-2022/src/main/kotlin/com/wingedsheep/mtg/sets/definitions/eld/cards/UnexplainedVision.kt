package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Unexplained Vision
 * {4}{U}
 * Sorcery
 * Draw three cards.
 * Adamant — If at least three blue mana was spent to cast this spell, scry 3.
 *
 * Adamant counts blue mana spent on the whole cost, read off this spell's own cast payment via
 * [DynamicAmounts.manaOfColorSpent] (see Turn into a Pumpkin).
 */
val UnexplainedVision = card("Unexplained Vision") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw three cards.\n" +
        "Adamant — If at least three blue mana was spent to cast this spell, scry 3."

    spell {
        effect = Effects.DrawCards(3) then Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.manaOfColorSpent(Color.BLUE),
                ComparisonOperator.GTE,
                3,
            ),
            Effects.Scry(3),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Bram Sels"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1de1d84-e548-409d-b9ec-d2665477b43d.jpg?1783932648"
    }
}
