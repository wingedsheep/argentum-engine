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
 * Slaying Fire
 * {2}{R}
 * Instant
 * Slaying Fire deals 3 damage to any target.
 * Adamant — If at least three red mana was spent to cast this spell, it deals 4 damage instead.
 *
 * Adamant reads this spell's own cast payment ([DynamicAmounts.manaOfColorSpent], as Searing
 * Barrage does) and picks the damage amount with [DynamicAmounts.conditional].
 */
val SlayingFire = card("Slaying Fire") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Slaying Fire deals 3 damage to any target.\n" +
        "Adamant — If at least three red mana was spent to cast this spell, it deals 4 damage instead."

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(
            DynamicAmounts.conditional(
                Conditions.CompareAmounts(
                    DynamicAmounts.manaOfColorSpent(Color.RED),
                    ComparisonOperator.GTE,
                    3,
                ),
                4,
                3,
            ),
            t,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83b5b110-c430-4ffe-9fc1-8e6987f52d1e.jpg?1783932616"
    }
}
