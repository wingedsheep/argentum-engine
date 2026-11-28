package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Moorland Haunt
 * (no mana cost)
 * Land
 * {T}: Add {C}.
 * {W}{U}, {T}, Exile a creature card from your graveyard: Create a 1/1 white Spirit creature token with flying.
 */
val MoorlandHaunt = card("Moorland Haunt") {
    manaCost = ""
    colorIdentity = "UW"
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{W}{U}, {T}, Exile a creature card from your graveyard: Create a 1/1 white Spirit creature token with flying."
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{W}{U}"),
            Costs.Tap,
            Costs.ExileFromGraveyard(count = 1, filter = GameObjectFilter.Creature),
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d5569e3-278c-4cf3-860e-712010333fe6.jpg"
    }
}
