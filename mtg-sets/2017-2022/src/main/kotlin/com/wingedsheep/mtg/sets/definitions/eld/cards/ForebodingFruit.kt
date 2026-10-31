package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Foreboding Fruit
 * {2}{B}
 * Sorcery
 * Target player draws two cards and loses 2 life.
 * Adamant — If at least three black mana was spent to cast this spell, create a Food token.
 *
 * Sign in Blood's body plus the Adamant rider read off this spell's own cast payment
 * ([DynamicAmounts.manaOfColorSpent], as Cauldron's Gift does). The Food goes to the caster,
 * not the target player.
 */
val ForebodingFruit = card("Foreboding Fruit") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player draws two cards and loses 2 life.\n" +
        "Adamant — If at least three black mana was spent to cast this spell, create a Food token. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    spell {
        val player = target(Targets.Player)
        effect = Effects.DrawCards(2, player) then
            Effects.LoseLife(2, player) then
            Effects.If(
                Conditions.CompareAmounts(
                    DynamicAmounts.manaOfColorSpent(Color.BLACK),
                    ComparisonOperator.GTE,
                    3,
                ),
                Effects.CreateFood(),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Drew Baker"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5f79ec4-3722-4fda-824b-e80dc7608d01.jpg?1783932640"
    }
}
