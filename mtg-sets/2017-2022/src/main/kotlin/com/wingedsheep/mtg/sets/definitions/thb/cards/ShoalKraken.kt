package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Shoal Kraken
 * {4}{U}
 * Creature — Kraken
 * 3/5
 * Constellation — Whenever an enchantment you control enters, you may draw a card. If you do,
 * discard a card.
 *
 * Protean Thaumaturge's constellation watcher feeding a "may" loot.
 */
val ShoalKraken = card("Shoal Kraken") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Kraken"
    power = 3
    toughness = 5
    oracleText = "Constellation — Whenever an enchantment you control enters, you may draw a card. " +
        "If you do, discard a card."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        effect = Effects.May(Patterns.Hand.loot())
        description = "Constellation — Whenever an enchantment you control enters, you may draw a card. " +
            "If you do, discard a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Vincent Proce"
        flavorText = "It eats the things that eat the things that are drawn to the light."
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec538182-efba-48d0-91ad-457461b44748.jpg?1783931579"
        ruling(
            "2020-01-24",
            "A constellation ability triggers whenever an enchantment enters the battlefield under your " +
                "control for any reason. Enchantments with other card types, such as enchantment creatures, " +
                "will also cause constellation abilities to trigger."
        )
        ruling(
            "2020-01-24",
            "An Aura spell that has an illegal target when it tries to resolve doesn't resolve and is instead " +
                "put into its owner's graveyard. It doesn't enter the battlefield, so constellation abilities " +
                "don't trigger."
        )
    }
}
