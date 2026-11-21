package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Opal Lake Gatekeepers
 * {3}{U}
 * Creature — Vedalken Soldier
 * 2/4
 * When this creature enters, if you control two or more Gates, you may draw a card.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val OpalLakeGatekeepers = card("Opal Lake Gatekeepers") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Soldier"
    power = 2
    toughness = 4
    oracleText = "When this creature enters, if you control two or more Gates, you may draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouControlAtLeast(2, GameObjectFilter.Land.withSubtype("Gate"))
        effect = Effects.May(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Seb McKinnon"
        flavorText = "Sentries guard Ravnica's landmarks and outlying districts, unaware of these places' import to the maze."
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f43ac38f-5cd0-46cf-8623-d82cb8fb719b.jpg?1783940042"
    }
}
