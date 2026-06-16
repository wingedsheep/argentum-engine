package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Strip Mine
 * Land
 * {T}: Add {C}.
 * {T}, Sacrifice this land: Destroy target land.
 */
val StripMine = card("Strip Mine") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{T}, Sacrifice this land: Destroy target land."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        val land = target("target land", Targets.Land)
        effect = Effects.Destroy(land)
        description = "{T}, Sacrifice this land: Destroy target land."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "82a"
        artist = "Daniel Gelon"
        flavorText = "Unlike previous conflicts, the war between Urza and Mishra made Dominia itself a casualty of war."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7880157-7f27-4f1b-9cdc-ab36a6252376.jpg?1562943840"
    }
}
