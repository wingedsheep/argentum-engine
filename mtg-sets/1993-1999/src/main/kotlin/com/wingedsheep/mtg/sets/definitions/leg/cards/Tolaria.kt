package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tolaria
 * Legendary Land
 *
 * {T}: Add {U}.
 * {T}: Target creature loses banding and all "bands with other" abilities until end of turn.
 * Activate only during any upkeep step.
 */
val Tolaria = card("Tolaria") {
    typeLine = "Legendary Land"
    colorIdentity = "U"
    oracleText = "{T}: Add {U}.\n{T}: Target creature loses banding and all \"bands with other\" " +
        "abilities until end of turn. Activate only during any upkeep step."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Tap
        // Under the banding rules, a permanent that loses banding loses every "bands with other"
        // ability too, so the single BANDING removal covers both halves of the printed text.
        effect = Effects.RemoveKeyword(Keyword.BANDING, creature)
        restrictions = listOf(ActivationRestriction.DuringStep(Step.UPKEEP))
        description = "{T}: Target creature loses banding and all \"bands with other\" abilities " +
            "until end of turn. Activate only during any upkeep step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "308"
        artist = "Nicola Leonard"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d43c01b7-443d-4061-a934-6863d230c9b8.jpg?1783948022"
    }
}
