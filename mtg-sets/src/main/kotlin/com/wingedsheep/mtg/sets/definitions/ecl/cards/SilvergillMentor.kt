package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Costs
/**
 * Silvergill Mentor
 * {1}{U}
 * Creature — Merfolk Wizard
 * 2/1
 *
 * As an additional cost to cast this spell, behold a Merfolk or pay {2}.
 * (To behold a Merfolk, choose a Merfolk you control or reveal a Merfolk card from your hand.)
 * When this creature enters, create a 1/1 white and blue Merfolk creature token.
 */
val SilvergillMentor = card("Silvergill Mentor") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 1
    oracleText = "As an additional cost to cast this spell, behold a Merfolk or pay {2}. " +
        "(To behold a Merfolk, choose a Merfolk you control or reveal a Merfolk card from your hand.)\n" +
        "When this creature enters, create a 1/1 white and blue Merfolk creature token."

    additionalCost(
        Costs.additional.BeholdOrPay(
            filter = Filters.WithSubtype("Merfolk"),
            alternativeManaCost = "{2}"
        )
    )

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLUE),
            creatureTypes = setOf("Merfolk"),
            imageUri = "https://cards.scryfall.io/normal/front/4/c/4c5ad4e1-b489-4023-88ab-1200c5f26ffc.jpg?1767955704"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Iris Compiet"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6e37fe8-459c-4992-8ae9-f782cddab2fe.jpg?1768338090"
    }
}
