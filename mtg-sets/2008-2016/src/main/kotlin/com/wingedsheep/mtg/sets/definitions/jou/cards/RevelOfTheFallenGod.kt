package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Revel of the Fallen God
 * {3}{R}{R}{G}{G}
 * Sorcery
 *
 * Create four 2/2 red and green Satyr creature tokens with haste.
 *
 * Journey into Nyx printed no Satyr token, so the token borrows Theros's 2/2 red-green Satyr with
 * haste (the Xenagos token) rather than falling through to the engine-wide generic art.
 */
val RevelOfTheFallenGod = card("Revel of the Fallen God") {
    manaCost = "{3}{R}{R}{G}{G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Create four 2/2 red and green Satyr creature tokens with haste."

    spell {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.RED, Color.GREEN),
            creatureTypes = setOf("Satyr"),
            keywords = setOf(Keyword.HASTE),
            count = 4,
            imageUri = "https://cards.scryfall.io/normal/front/b/a/baa93038-2849-4c26-ab4f-1d50d276659f.jpg?1783939699",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Adam Paquette"
        flavorText = "\"Xenagos has fallen, and the world grows dark without him. What can ever fill that " +
            "darkness but our rage?\"\n—Guruthes, disciple of Xenagos"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1d2c774-aab9-4747-af91-da792ed7cfe1.jpg?1783939402"
    }
}
