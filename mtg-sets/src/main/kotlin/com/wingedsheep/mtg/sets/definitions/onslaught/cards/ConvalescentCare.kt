package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalEffect

/**
 * Convalescent Care
 * {1}{W}{W}
 * Enchantment
 * At the beginning of your upkeep, if you have 5 or less life, you gain 3 life and draw a card.
 */
val ConvalescentCare = card("Convalescent Care") {
    manaCost = "{1}{W}{W}"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, if you have 5 or less life, you gain 3 life and draw a card."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        triggerCondition = Conditions.LifeAtMost(5)
        effect = ConditionalEffect(
            condition = Conditions.LifeAtMost(5),
            effect = Effects.GainLife(3) then Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Greg Hildebrandt"
        flavorText = "Enlightenment comes most swiftly at life's end."
        imageUri = "https://cards.scryfall.io/large/front/4/8/48f3ad80-d000-496a-b704-d09e07981b6e.jpg?1562910180"
    }
}
