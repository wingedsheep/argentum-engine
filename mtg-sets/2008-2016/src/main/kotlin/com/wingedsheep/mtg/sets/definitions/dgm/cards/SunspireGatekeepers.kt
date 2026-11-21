package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sunspire Gatekeepers
 * {3}{W}
 * Creature — Human Soldier
 * 2/4
 * When this creature enters, if you control two or more Gates, create a 2/2 white Knight creature
 * token with vigilance.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val SunspireGatekeepers = card("Sunspire Gatekeepers") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 4
    oracleText = "When this creature enters, if you control two or more Gates, create a 2/2 white Knight " +
        "creature token with vigilance."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouControlAtLeast(2, GameObjectFilter.Land.withSubtype("Gate"))
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Knight"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/3/d/3df61c33-38b3-4549-9e5d-ae1b5c1afd62.jpg?1783915260"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Chippy"
        flavorText = "\"You will pass with respect, or you will not pass at all.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a3bc6b9-475b-4257-a3bc-1a0b70d45f79.jpg?1783940043"
    }
}
