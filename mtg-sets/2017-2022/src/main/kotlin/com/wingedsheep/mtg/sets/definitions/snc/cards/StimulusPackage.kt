package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Stimulus Package
 * {2}{R}{G}
 * Enchantment
 * When this enchantment enters, create two Treasure tokens.
 * Sacrifice a Treasure: Create a 1/1 green and white Citizen creature token.
 *
 * The sacrifice cost is the Professional Face-Breaker shape: any Treasure you control, not only the
 * two this made. The Citizen token is the shared Courier's Briefcase spelling.
 */
val StimulusPackage = card("Stimulus Package") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "GR"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create two Treasure tokens. " +
        "(They're artifacts with \"{T}, Sacrifice this token: Add one mana of any color.\")\n" +
        "Sacrifice a Treasure: Create a 1/1 green and white Citizen creature token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateTreasure(2)
    }

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Artifact.withSubtype("Treasure"))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Citizen"),
        )
        description = "Sacrifice a Treasure: Create a 1/1 green and white Citizen creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "225"
        artist = "Robin Olausson"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecbe7459-8613-4ab8-84dc-deab19c08511.jpg?1783923068"
    }
}
