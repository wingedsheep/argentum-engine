package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Gravpack Monoist
 * {2}{B}
 * Creature — Human Scout
 * Flying
 * When this creature dies, create a tapped 2/2 colorless Robot artifact creature token.
 */
val GravpackMonoist = card("Gravpack Monoist") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Human Scout"
    power = 2
    toughness = 1
    oracleText = "Flying\nWhen this creature dies, create a tapped 2/2 colorless Robot artifact creature token."

    keywords(Keyword.FLYING)

    // When this creature dies, create a tapped 2/2 colorless Robot artifact creature token
    triggeredAbility {
        trigger = Triggers.Dies
        effect = CreateTokenEffect(
            name = "Robot",
            power = 2,
            toughness = 2,
            colors = setOf(),
            creatureTypes = setOf("Robot"),
            artifactToken = true,
            tapped = true,
            imageUri = "https://cards.scryfall.io/normal/front/c/4/c46f9a07-005c-44b7-8057-b2f00b274dd6.jpg?1756281130"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Edgar Sánchez Hidalgo"
        flavorText = "Her food stores will run out before her fuel, leaving only her mechan gravpack to carry her remains to the Next Eternity."
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a968f01-36ef-4cf6-b1db-630c9cde2064.jpg?1752946977"
    }
}
