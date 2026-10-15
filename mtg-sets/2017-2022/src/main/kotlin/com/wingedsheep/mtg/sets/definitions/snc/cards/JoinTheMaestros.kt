package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Join the Maestros
 * {4}{B}
 * Sorcery
 * Casualty 2 (As you cast this spell, you may sacrifice a creature with power 2 or greater. When you do, copy this spell.)
 * Create a 4/3 black Ogre Warrior creature token.
 */
val JoinTheMaestros = card("Join the Maestros") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Casualty 2 (As you cast this spell, you may sacrifice a creature with power 2 or greater. When you do, copy this spell.)\n" +
        "Create a 4/3 black Ogre Warrior creature token."

    keywordAbility(KeywordAbility.casualty(2))

    spell {
        effect = Effects.CreateToken(
            power = 4,
            toughness = 3,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Ogre", "Warrior")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Josh Hass"
        flavorText = "\"We're family now, big guy.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff2665f7-6cf8-4f9b-810e-17e264753225.jpg"
    }
}
