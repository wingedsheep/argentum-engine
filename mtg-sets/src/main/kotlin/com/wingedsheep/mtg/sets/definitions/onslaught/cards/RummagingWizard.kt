package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost

/**
 * Rummaging Wizard
 * {3}{U}
 * Creature — Human Wizard
 * 2/2
 * {2}{U}: Surveil 1. (Look at the top card of your library. You may put that card
 * into your graveyard.)
 */
val RummagingWizard = card("Rummaging Wizard") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "{2}{U}: Surveil 1. (Look at the top card of your library. You may put that card into your graveyard.)"

    activatedAbility {
        cost = AbilityCost.Mana(ManaCost.parse("{2}{U}"))
        effect = Effects.Surveil(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Glen Angus"
        flavorText = "\"I've got everything you'd ever need right here. Just give me some time to find it.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad96e158-bf2b-4f3e-9692-0f79efdd94f5.jpg?1562932018"
    }
}
