package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Henge of Ramos
 *
 * Land
 *
 * {T}: Add {C}.
 * {2}, {T}: Add one mana of any color.
 *
 * Both abilities are mana abilities: the second one costs mana but adds it, so CR 605.1a still
 * applies. "Add one mana of any color" is [Effects.AddAnyColorMana] with its default single mana.
 */
val HengeOfRamos = card("Henge of Ramos") {
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{2}, {T}: Add one mana of any color."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.AddAnyColorMana()
        manaAbility = true
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "318"
        artist = "Edward P. Beard, Jr."
        flavorText = "It is the hub. We are the wheel.\n" +
            "—Dryad saying"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/0582b42f-5ae5-4be2-ba2d-ed62b3cb20c5.jpg"
    }
}
