package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Witch's Oven — Throne of Eldraine #237
 * {1} · Artifact
 *
 * {T}, Sacrifice a creature: Create a Food token. If the sacrificed creature's toughness was 4 or
 * greater, create two Food tokens instead.
 *
 * The sacrificed creature is read through [DynamicAmounts.sacrificedToughness] — its last-known
 * toughness as it was sacrificed to pay the cost (Feed the Pack / Shilgengar's path) — and an
 * [Effects.If] picks two Foods over one. The sacrifice is part of the cost, so nobody can respond
 * between activation and the sacrifice.
 */
val WitchsOven = card("Witch's Oven") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}, Sacrifice a creature: Create a Food token. If the sacrificed creature's toughness " +
        "was 4 or greater, create two Food tokens instead. (They're artifacts with \"{2}, {T}, Sacrifice " +
        "this token: You gain 3 life.\")"

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Sacrifice(GameObjectFilter.Creature))
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.sacrificedToughness(),
                ComparisonOperator.GTE,
                4,
            ),
            then = Effects.CreateFood(2),
            otherwise = Effects.CreateFood(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "Alexander Forssberg"
        flavorText = "The wafting smells are both scrumptious and suspicious."
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04ef8493-d986-45f8-a718-617b028f7ad4.jpg?1783932580"
        ruling(
            "2019-10-04",
            "No player may take actions between the time you announce you're activating Witch's Oven's " +
                "ability and the time you sacrifice a creature. Notably, they can't try to remove or shrink " +
                "the creature you'd like to bake."
        )
    }
}
