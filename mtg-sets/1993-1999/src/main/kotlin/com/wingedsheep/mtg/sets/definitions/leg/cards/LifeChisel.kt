package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Life Chisel
 * {4}
 * Artifact
 * Sacrifice a creature: You gain life equal to the sacrificed creature's toughness.
 * Activate only during your upkeep.
 */
val LifeChisel = card("Life Chisel") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Sacrifice a creature: You gain life equal to the sacrificed creature's toughness. " +
        "Activate only during your upkeep."

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Creature)
        effect = Effects.GainLife(DynamicAmounts.sacrificedToughness(), EffectTarget.Controller)
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.OnlyDuringYourTurn,
                ActivationRestriction.DuringStep(Step.UPKEEP)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "283"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50052f9a-d667-4a96-a9b1-b4169ee495e6.jpg?1783948027"
    }
}
