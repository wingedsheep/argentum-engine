package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Saruli Gatekeepers
 * {3}{G}
 * Creature — Elf Warrior
 * 2/4
 * When this creature enters, if you control two or more Gates, you gain 7 life.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val SaruliGatekeepers = card("Saruli Gatekeepers") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    power = 2
    toughness = 4
    oracleText = "When this creature enters, if you control two or more Gates, you gain 7 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouControlAtLeast(2, GameObjectFilter.Land.withSubtype("Gate"))
        effect = Effects.GainLife(7)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Chris Rahn"
        flavorText = "In Ravnica's wilder districts, nature still stirs, and its protectors still mind their posts."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/471a5b1d-e2e5-4d90-b72a-ffae81ad6602.jpg?1783940035"
    }
}
