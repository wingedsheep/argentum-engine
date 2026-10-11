package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Overflowing Basin
 * Land
 * {1}, {T}: Add {G}{U}.
 */
val OverflowingBasin = card("Overflowing Basin") {
    manaCost = ""
    colorIdentity = "GU"
    typeLine = "Land"
    oracleText = "{1}, {T}: Add {G}{U}."
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddMana(Color.GREEN, 1) then Effects.AddMana(Color.BLUE, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Marina Ortega Lorente"
        flavorText = "Unfortunately for Vault 34's remaining residents, a battle over the armory resulted in flooding, a leaking reactor, and broken doors."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75372ba1-85fb-4923-a62f-4050761c1471.jpg?1783912373"
    }
}
