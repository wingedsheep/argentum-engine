package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Nuka-Cola Vending Machine
 * {3}
 * Artifact
 *
 * {1}, {T}: Create a Food token.
 * Whenever you sacrifice a Food, create a tapped Treasure token.
 *
 * The sacrifice trigger watches any Food artifact you sacrifice, token or not, for any reason
 * (the same `Triggers.you.sacrifices` shape as Sierra, Nuka's Biggest Fan).
 */
val NukaColaVendingMachine = card("Nuka-Cola Vending Machine") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "Whenever you sacrifice a Food, create a tapped Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.CreateFood(1)
    }

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Artifact.withSubtype("Food"))
        effect = Effects.CreateTreasure(tapped = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Anthony Devine"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/405df7c7-0e37-499e-9826-6bf9f5db7a40.jpg?1783912378"
    }
}
