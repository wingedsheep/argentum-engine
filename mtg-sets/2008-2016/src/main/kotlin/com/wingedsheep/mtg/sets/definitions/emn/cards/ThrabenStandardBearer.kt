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
 *
 * Eldritch Moon's own token sheet has no Human Soldier; the token is the one from Shadows over
 * Innistrad's sheet, so its art is pinned here (the differential ignores token art).
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
            creatureTypes = setOf("Human", "Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/d/9/d9cbf36e-4044-4f08-9bae-f0dcb2455716.jpg?1783937683"
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
