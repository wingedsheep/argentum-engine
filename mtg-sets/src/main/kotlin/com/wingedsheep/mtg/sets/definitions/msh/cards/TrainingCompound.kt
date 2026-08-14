package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Training Compound
 * Land
 * {T}: Add {C}.
 * {T}: Add {R} or {G}. Activate only if this land entered this turn or if you control a basic
 * land.
 *
 * Red/green sibling of [HiddenLair] in MSH's conditional-dual-land cycle — same shape, same
 * [ActivationRestriction.OnlyIfCondition] gate.
 */
val TrainingCompound = card("Training Compound") {
    typeLine = "Land"
    colorIdentity = "RG"
    oracleText = "{T}: Add {C}.\n{T}: Add {R} or {G}. Activate only if this land entered this " +
        "turn or if you control a basic land."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice(ManaColorSet.Specific(setOf(Color.RED, Color.GREEN)))
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.Any(
                    Conditions.SourceEnteredThisTurn,
                    Conditions.YouControl(Filters.BasicLand),
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "275"
        artist = "Jonas De Ro"
        flavorText = "\"I've been authorized to set up a California expansion team. Who's " +
            "with me?\"\n—Hawkeye, Clint Barton"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c91e28db-307f-462a-88aa-581d10e77f10.jpg?1783902882"
    }
}
