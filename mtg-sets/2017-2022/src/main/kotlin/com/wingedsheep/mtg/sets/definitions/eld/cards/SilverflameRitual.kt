package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Silverflame Ritual
 * {3}{W}
 * Sorcery
 * Put a +1/+1 counter on each creature you control.
 * Adamant — If at least three white mana was spent to cast this spell, creatures you control gain
 * vigilance until end of turn.
 *
 * Adamant counts white mana spent on the whole cost, generic included, read off this spell's own
 * cast payment via [DynamicAmounts.manaOfColorSpent]. A copy or a free cast spent no mana and grants
 * no vigilance; the counters are unconditional.
 */
val SilverflameRitual = card("Silverflame Ritual") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on each creature you control.\n" +
        "Adamant — If at least three white mana was spent to cast this spell, creatures you control gain vigilance until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            Filters.Group.creaturesYouControl,
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        ) then Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.manaOfColorSpent(Color.WHITE),
                ComparisonOperator.GTE,
                3,
            ),
            Patterns.Group.grantKeywordToAll(Keyword.VIGILANCE, Filters.Group.creaturesYouControl),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Jason Felix"
        flavorText = "Tempered by fire and united in virtue, the knights of Ardenvale fight with a single, unbreakable will."
        imageUri = "https://cards.scryfall.io/normal/front/c/a/cad7efd1-5f53-4466-9a02-c474c265298a.jpg?1783932666"
    }
}
