package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Breeding Pit
 * {3}{B}
 * Enchantment
 * At the beginning of your upkeep, sacrifice this enchantment unless you pay {B}{B}.
 * At the beginning of your end step, create a 0/1 black Thrull creature token.
 *
 * The Thrull art is registered on [com.wingedsheep.mtg.sets.definitions.fem.FallenEmpiresSet].
 */
val BreedingPit = card("Breeding Pit") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, sacrifice this enchantment unless you pay {B}{B}.\n" +
        "At the beginning of your end step, create a 0/1 black Thrull creature token."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = PayOrSufferEffect(
            cost = Costs.pay.Mana("{B}{B}"),
            suffer = SacrificeSelfEffect,
        )
        description = "At the beginning of your upkeep, sacrifice this enchantment unless you pay {B}{B}."
    }

    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.CreateToken(
            power = 0,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Thrull")
        )
        description = "At the beginning of your end step, create a 0/1 black Thrull creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Anson Maddocks"
        flavorText = "The Thrulls bred at a terrifying pace. In the end, they overwhelmed the Order of the Ebon Hand."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0d7e85f-eba5-4fc5-9fc0-109109d368aa.jpg?1783947903"
    }
}
