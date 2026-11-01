package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantHexproofToController

/**
 * Teyo, the Shieldmage
 * {2}{W}
 * Legendary Planeswalker — Teyo
 * Loyalty 5
 *
 * You have hexproof. (You can't be the target of spells or abilities your opponents control.)
 * −2: Create a 0/3 white Wall creature token with defender.
 */
val TeyoTheShieldmage = card("Teyo, the Shieldmage") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Teyo"
    startingLoyalty = 5
    oracleText = "You have hexproof. (You can't be the target of spells or abilities your opponents control.)\n" +
        "−2: Create a 0/3 white Wall creature token with defender."

    // You have hexproof.
    staticAbility {
        ability = GrantHexproofToController
    }

    // −2: Create a 0/3 white Wall creature token with defender.
    loyaltyAbility(-2) {
        effect = Effects.CreateToken(
            power = 0,
            toughness = 3,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Wall"),
            keywords = setOf(Keyword.DEFENDER),
            imageUri = "https://cards.scryfall.io/normal/front/e/8/e8ae8c31-70cb-44e1-a105-f4d29f1f9356.jpg?1783933354"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a974571c-d8d5-4860-9077-2023e5f32a24.jpg?1783933473"
    }
}
