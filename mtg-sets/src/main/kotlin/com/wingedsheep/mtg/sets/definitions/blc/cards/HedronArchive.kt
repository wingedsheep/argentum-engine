package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Hedron Archive
 * {4}
 * Artifact
 *
 * {T}: Add {C}{C}.
 * {2}, {T}, Sacrifice this artifact: Draw two cards.
 */
val HedronArchive = card("Hedron Archive") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}{C}.\n" +
        "{2}, {T}, Sacrifice this artifact: Draw two cards."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.SacrificeSelf,
        )
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "275"
        artist = "Craig J Spearing"
        flavorText = "\"You've begun to understand the hedrons' true purpose,\" said Ugin. \"The Eldrazi can be imprisoned.\"\n" +
            "\"And how did that work out last time?\" asked Jace."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/228ce785-033c-4973-89c7-4616a5b9c9d2.jpg?1721429583"
    }
}
