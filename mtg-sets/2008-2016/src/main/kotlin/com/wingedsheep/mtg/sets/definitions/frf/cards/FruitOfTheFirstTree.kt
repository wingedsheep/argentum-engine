package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Fruit of the First Tree
 * {3}{G}
 * Enchantment — Aura
 * Enchant creature
 * When enchanted creature dies, you gain X life and draw X cards, where X is its toughness.
 *
 * X is the dying creature's last-known toughness, read off the zone-change trigger context by
 * [DynamicAmounts.triggeringToughness]. "You" is the Aura's controller, even when it enchants an
 * opponent's creature. X of 0 or less gains and draws nothing (the executors no-op on it).
 */
val FruitOfTheFirstTree = card("Fruit of the First Tree") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When enchanted creature dies, you gain X life and draw X cards, where X is its toughness."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.GainLife(DynamicAmounts.triggeringToughness()) then
            Effects.DrawCards(DynamicAmounts.triggeringToughness())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Ryan Yee"
        flavorText = "\"I will be the ancestor my descendants call upon for aid. On that day I will take up my sword for my family once more.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d3e6a6a-4a19-41e1-b29e-a8f19838efe3.jpg?1783938678"
        ruling(
            "2014-11-24",
            "Use the creature's toughness when it left the battlefield to determine the value of X. If that number is 0 or less, you won't gain life or draw cards. (You won't lose life or discard cards either.)"
        )
        ruling(
            "2014-11-24",
            "Fruit of the First Tree can enchant any creature, but Fruit of the First Tree's controller will gain life and draw cards."
        )
    }
}
