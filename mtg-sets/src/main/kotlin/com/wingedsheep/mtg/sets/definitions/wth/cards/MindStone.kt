package com.wingedsheep.mtg.sets.definitions.wth.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Mind Stone
 * {2}
 * Artifact
 *
 * {T}: Add {C}.
 * {1}, {T}, Sacrifice this artifact: Draw a card.
 */
val MindStone = card("Mind Stone") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}.\n" +
        "{1}, {T}, Sacrifice this artifact: Draw a card."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Tap,
            Costs.SacrificeSelf,
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Adam Rex"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/162e81d3-6cd4-4cb8-8ed8-cfbd8d34ca71.jpg"
    }
}
