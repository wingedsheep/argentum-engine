package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Army of the Damned
 * {5}{B}{B}{B}
 * Sorcery
 * Create thirteen tapped 2/2 black Zombie creature tokens.
 * Flashback {7}{B}{B}{B}
 */
val ArmyOfTheDamned = card("Army of the Damned") {
    manaCost = "{5}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Create thirteen tapped 2/2 black Zombie creature tokens.\n" +
        "Flashback {7}{B}{B}{B} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            count = 13,
            tapped = true
        )
    }

    keywordAbility(KeywordAbility.flashback("{7}{B}{B}{B}"))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "87"
        artist = "Ryan Pancoast"
        flavorText = "Sometimes death comes knocking. Sometimes it tears down the walls."
        imageUri = "https://cards.scryfall.io/normal/front/2/6/260a4544-a1eb-4d07-943f-0401ae288e13.jpg?1783940961"
    }
}
