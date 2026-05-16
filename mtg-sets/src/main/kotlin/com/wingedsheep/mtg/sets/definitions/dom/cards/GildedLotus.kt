package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Gilded Lotus
 * {5}
 * Artifact
 * {T}: Add three mana of any one color.
 */
val GildedLotus = card("Gilded Lotus") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add three mana of any one color."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(3)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Volkan Baǵa"
        flavorText = "\"The perfection of the lotus reminds me of my hopes for this world . . . and my failures. I will not rest until I've atoned for them.\" —Karn"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a487e208-8493-4bca-8c44-284d89c66b15.jpg?1562740681"
    }
}
