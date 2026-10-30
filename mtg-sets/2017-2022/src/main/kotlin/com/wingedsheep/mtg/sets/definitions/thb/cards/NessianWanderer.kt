package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Nessian Wanderer
 * {1}{G}
 * Creature — Satyr Scout
 * 1/3
 * Constellation — Whenever an enchantment you control enters, look at the top three cards of your
 * library. You may reveal a land card from among them and put that card into your hand. Put the
 * rest on the bottom of your library in a random order.
 *
 * Constellation is an ability word: a plain "an enchantment you control enters" trigger (Pious
 * Wayfarer), paying off with Quandrix Apprentice's look-at-three-take-a-land pattern, the rest
 * going to the bottom in a random order.
 */
val NessianWanderer = card("Nessian Wanderer") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Satyr Scout"
    power = 1
    toughness = 3
    oracleText = "Constellation — Whenever an enchantment you control enters, look at the top three " +
        "cards of your library. You may reveal a land card from among them and put that card into your " +
        "hand. Put the rest on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 3,
            filter = GameObjectFilter.Land,
            prompt = "You may reveal a land card from among them and put it into your hand",
            restOrder = CardOrder.Random
        )
        description = "Constellation — Whenever an enchantment you control enters, look at the top three " +
            "cards of your library. You may reveal a land card from among them and put that card into " +
            "your hand. Put the rest on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "183"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50d50576-c3ee-479a-8533-6c147f5cb1c4.jpg?1783931536"
        ruling("2020-01-24", "A constellation ability triggers whenever an enchantment enters the battlefield under your control for any reason. Enchantments with other card types, such as enchantment creatures, will also cause constellation abilities to trigger.")
        ruling("2020-01-24", "An Aura spell that has an illegal target when it tries to resolve doesn't resolve and is instead put into its owner's graveyard. It doesn't enter the battlefield, so constellation abilities don't trigger.")
    }
}
