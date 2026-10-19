package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hammerheim
 * Legendary Land
 *
 * {T}: Add {R}.
 * {T}: Target creature loses all landwalk abilities until end of turn.
 *
 * "All landwalk abilities" is the whole landwalk family ([Keyword.LANDWALK]: the five basic
 * landwalks, desertwalk and nonbasic landwalk), each removed for the turn whether printed or
 * granted — the Scarwood Hag "loses forestwalk" shape, once per member of the family.
 */
val Hammerheim = card("Hammerheim") {
    typeLine = "Legendary Land"
    colorIdentity = "R"
    oracleText = "{T}: Add {R}.\n{T}: Target creature loses all landwalk abilities until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Tap
        effect = Effects.Composite(
            Keyword.LANDWALK.map { Effects.RemoveKeyword(it, target = creature) },
            descriptionOverride = "Target creature loses all landwalk abilities until end of turn."
        )
        description = "{T}: Target creature loses all landwalk abilities until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "302"
        artist = "Bryon Wackwitz"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/2816d30e-1e52-4323-b30e-1688fba23368.jpg?1783948022"
    }
}
