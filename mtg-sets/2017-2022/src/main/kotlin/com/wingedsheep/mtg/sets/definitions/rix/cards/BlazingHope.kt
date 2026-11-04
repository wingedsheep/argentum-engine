package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Blazing Hope
 * {W}
 * Instant
 *
 * Exile target creature with power greater than or equal to your life total.
 *
 * The power restriction is a target filter, so it is checked both when the spell is cast and again
 * on resolution (CR 608.2b): if your life total rises above the creature's power in between, the
 * spell has no legal target and does nothing.
 */
val BlazingHope = card("Blazing Hope") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature with power greater than or equal to your life total."

    spell {
        val creature = target(
            TargetFilter.Creature.compareNumericProperty(
                CardNumericProperty.POWER,
                ComparisonOperator.GTE,
                DynamicAmounts.yourLifeTotal(),
            )
        )
        effect = Effects.Exile(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Clint Cearley"
        flavorText = "\"No shadow remains for you to hide in.\"\n—Caparocti Sunborn"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6beac36-e12f-408d-a37b-a70e0e6c52d8.jpg?1783935342"
    }
}
