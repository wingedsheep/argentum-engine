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
 * Dark Fortress
 * Land
 * {T}: Add {C}.
 * {T}: Add {B} or {R}. Activate only if this land entered this turn or if you control a basic
 * land.
 *
 * Black/red sibling of [HiddenLair] in MSH's conditional-dual-land cycle — same shape, same
 * [ActivationRestriction.OnlyIfCondition] gate.
 */
val DarkFortress = card("Dark Fortress") {
    typeLine = "Land"
    colorIdentity = "BR"
    oracleText = "{T}: Add {C}.\n{T}: Add {B} or {R}. Activate only if this land entered this " +
        "turn or if you control a basic land."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice(ManaColorSet.Specific(setOf(Color.BLACK, Color.RED)))
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
        collectorNumber = "264"
        artist = "Arthur Yuan"
        flavorText = "\"You talk of freedoms. Liberty. Whose hands do you think built these " +
            "battlements? In their hearts, peasants want to be ruled.\"\n—Baron Zemo"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c16fd43c-7c47-4c1b-860f-91146532e89d.jpg?1783902885"
    }
}
