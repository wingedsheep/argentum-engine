package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.FreeCyclingCosts
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * New Perspectives — Amonkhet #63
 * {5}{U} · Enchantment · Rare
 *
 * When this enchantment enters, draw three cards.
 * As long as you have seven or more cards in hand, you may pay {0} rather than pay cycling costs.
 *
 * The {0} is an alternative cost ([FreeCyclingCosts]), checked as each cycling ability is
 * activated — the card being cycled is still in hand and counts toward the seven. Typecycling is
 * cycling, so it is free too.
 */
val NewPerspectives = card("New Perspectives") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, draw three cards.\n" +
        "As long as you have seven or more cards in hand, you may pay {0} rather than pay cycling costs."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(3)
    }

    staticAbility {
        condition = Conditions.CompareAmounts(DynamicAmounts.cardsInYourHand(), ComparisonOperator.GTE, 7)
        ability = FreeCyclingCosts
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Darek Zabrocki"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c56b5290-997d-44de-ac83-4bc9a9911858.jpg?1783936518"
    }
}
