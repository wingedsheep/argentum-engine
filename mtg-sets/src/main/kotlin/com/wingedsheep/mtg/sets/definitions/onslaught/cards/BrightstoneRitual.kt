package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Brightstone Ritual
 * {R}
 * Instant
 * Add {R} for each Goblin on the battlefield.
 */
val BrightstoneRitual = card("Brightstone Ritual") {
    manaCost = "{R}"
    typeLine = "Instant"

    spell {
        effect = Effects.AddMana(Color.RED, DynamicAmounts.creaturesWithSubtype(Subtype("Goblin")))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "191"
        artist = "Wayne England"
        flavorText = "Wizards fought over the stone to exploit its power. Goblins fight over it because it's shiny."
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b08b0a6-c94e-4407-8a24-c8202497b5f2.jpg?1562916460"
    }
}
