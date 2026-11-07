package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Thraben Standard Bearer (Eldritch Moon #48)
 * {W}
 * Creature — Human Soldier
 * 1/1
 *
 * {1}{W}, {T}, Discard a card: Create a 1/1 white Human Soldier creature token.
 */
val ThrabenStandardBearer = card("Thraben Standard Bearer") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "{1}{W}, {T}, Discard a card: Create a 1/1 white Human Soldier creature token."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap, Costs.DiscardCard)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Volkan Baǵa"
        flavorText = "The uncorrupted rallied under Thalia's leadership and Sigarda's banner."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15b7cd60-e3b9-4dd8-ad3c-a687f642147f.jpg"
    }
}
