package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Foundry of the Consuls
 * Land
 * {T}: Add {C}.
 * {5}, {T}, Sacrifice this land: Create two 1/1 colorless Thopter artifact creature tokens with flying.
 */
val FoundryOfTheConsuls = card("Foundry of the Consuls") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{5}, {T}, Sacrifice this land: Create two 1/1 colorless Thopter artifact creature tokens with flying."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            count = 2,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "247"
        artist = "Sam Burley"
        flavorText = "All around the foundry, automatons unhitched themselves from their mounts and pivoted to face Chandra."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73f29e03-0a71-46cd-992e-7baa61f16ac5.jpg?1783938306"
    }
}
