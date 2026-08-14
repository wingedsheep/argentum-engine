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
 * Gleaming Bastion
 * Land
 * {T}: Add {C}.
 * {T}: Add {W} or {U}. Activate only if this land entered this turn or if you control a basic
 * land.
 *
 * White/blue sibling of [HiddenLair] in MSH's conditional-dual-land cycle — same shape, same
 * [ActivationRestriction.OnlyIfCondition] gate.
 */
val GleamingBastion = card("Gleaming Bastion") {
    typeLine = "Land"
    colorIdentity = "WU"
    oracleText = "{T}: Add {C}.\n{T}: Add {W} or {U}. Activate only if this land entered this " +
        "turn or if you control a basic land."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice(ManaColorSet.Specific(setOf(Color.WHITE, Color.BLUE)))
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
        collectorNumber = "267"
        artist = "Arthur Yuan"
        flavorText = "\"Welcome to S.H.I.E.L.D. Impressive, huh? Fury needed someplace to park " +
            "his car.\"\n—Agent Maria Hill"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9131bf5-17e3-4aa6-97ed-ed6426b247d0.jpg?1783902884"
    }
}
