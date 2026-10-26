package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Karn's Bastion
 * Land
 * {T}: Add {C}.
 * {4}, {T}: Proliferate.
 */
val KarnsBastion = card("Karn's Bastion") {
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{4}, {T}: Proliferate. (Choose any number of permanents and/or players, then give each another " +
        "counter of each kind already there.)"

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddColorlessMana(1)
        description = "{T}: Add {C}."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.Proliferate()
        description = "{4}, {T}: Proliferate."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "248"
        artist = "Adam Paquette"
        flavorText = "\"I can't protect everyone here. But those I can, I will.\"\n—Karn"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9d895af-7e8c-419f-bc5d-5596083fbfb6.jpg?1783933368"
    }
}
