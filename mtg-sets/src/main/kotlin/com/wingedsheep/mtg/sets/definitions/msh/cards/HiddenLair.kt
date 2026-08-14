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
 * Hidden Lair
 * Land
 * {T}: Add {C}.
 * {T}: Add {U} or {B}. Activate only if this land entered this turn or if you control a basic
 * land.
 *
 * The colored-mana ability is fenced behind an [ActivationRestriction.OnlyIfCondition] gating
 * on [Conditions.SourceEnteredThisTurn] OR [Conditions.YouControl] a basic land — so it's always
 * available the turn it enters, and afterward stays available only as long as a real basic land
 * backs it up.
 */
val HiddenLair = card("Hidden Lair") {
    typeLine = "Land"
    colorIdentity = "UB"
    oracleText = "{T}: Add {C}.\n{T}: Add {U} or {B}. Activate only if this land entered this " +
        "turn or if you control a basic land."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice(ManaColorSet.Specific(setOf(Color.BLUE, Color.BLACK)))
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
        collectorNumber = "269"
        artist = "Pavel Kolomeyets"
        flavorText = "While the Avengers worried over the threat of Ultron-19, in a remote " +
            "bunker, the construction of Ultron-2458 neared completion."
        imageUri = "https://cards.scryfall.io/normal/front/0/7/0742ddb6-71ed-444e-91ad-84f876725a4a.jpg?1783902883"
    }
}
