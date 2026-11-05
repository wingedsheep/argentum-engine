package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Shapers' Sanctuary
 * {G}
 * Enchantment
 * Whenever a creature you control becomes the target of a spell or ability an opponent controls,
 * you may draw a card.
 */
val ShapersSanctuary = card("Shapers' Sanctuary") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control becomes the target of a spell or ability an opponent controls, you may draw a card."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).becomesTarget(byOpponent = true)
        effect = Effects.May(Effects.DrawCards(1))
        description = "Whenever a creature you control becomes the target of a spell or ability " +
            "an opponent controls, you may draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "206"
        artist = "Zezhou Chen"
        flavorText = "Protection is woven into every vine and branch."
        imageUri = "https://cards.scryfall.io/normal/front/9/3/930240dc-9c8c-4857-b6ce-85f1689c60e5.jpg?1783935719"
        ruling("2017-09-29", "The triggered ability of Shapers' Sanctuary resolves before the spell or ability that caused it to trigger.")
        ruling("2017-09-29", "Players can cast spells and activate abilities after the triggered ability of Shapers' Sanctuary resolves but before the spell or ability that caused it to trigger does. Notably, the card you draw may be able to counter that spell or ability.")
    }
}
