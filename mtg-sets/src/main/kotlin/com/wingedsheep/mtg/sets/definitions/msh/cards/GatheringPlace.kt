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
 * Gathering Place
 * Land
 * {T}: Add {C}.
 * {T}: Add {G} or {W}. Activate only if this land entered this turn or if you control a basic
 * land.
 *
 * Green/white sibling of [HiddenLair] in MSH's conditional-dual-land cycle — same shape, same
 * [ActivationRestriction.OnlyIfCondition] gate.
 */
val GatheringPlace = card("Gathering Place") {
    typeLine = "Land"
    colorIdentity = "WG"
    oracleText = "{T}: Add {C}.\n{T}: Add {G} or {W}. Activate only if this land entered this " +
        "turn or if you control a basic land."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice(ManaColorSet.Specific(setOf(Color.GREEN, Color.WHITE)))
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
        collectorNumber = "266"
        artist = "Pablo Mendoza"
        flavorText = "\"Will your friends be staying the night, Master Stark?\"\n—Edwin " +
            "Jarvis, Avengers Mansion butler"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/081cdbd0-5081-4a9e-90ba-f5baf4ac137e.jpg?1783902886"
    }
}
