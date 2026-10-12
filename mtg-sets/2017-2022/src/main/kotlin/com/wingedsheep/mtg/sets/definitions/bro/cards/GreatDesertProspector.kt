package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Great Desert Prospector
 * {4}{W}
 * Creature — Human Artificer
 * 3/2
 * When this creature enters, create a tapped Powerstone token for each other creature you control.
 *
 * The count is read at resolution and excludes the Prospector itself by identity (`excludeSelf`),
 * so it stays right even if the Prospector has left the battlefield by then.
 */
val GreatDesertProspector = card("Great Desert Prospector") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Artificer"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, create a tapped Powerstone token for each other creature you control. (They're artifacts with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreatePowerstone(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature, excludeSelf = true).count(),
            tapped = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Néstor Ossandón Leal"
        flavorText = "\"Hello, beautiful.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc83d586-bf46-437f-a5a3-996bd62d5866.jpg?1783920132"
    }
}
