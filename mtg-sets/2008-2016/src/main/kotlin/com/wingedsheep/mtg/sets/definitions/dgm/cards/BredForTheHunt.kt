package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Bred for the Hunt
 * {1}{G}{U}
 * Enchantment
 * Whenever a creature you control with a +1/+1 counter on it deals combat damage to a player,
 * you may draw a card.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 *
 * The trigger is the enchantment's own (it watches your creatures), not one granted to them: a
 * creature that loses all abilities still draws you a card.
 */
val BredForTheHunt = card("Bred for the Hunt") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control with a +1/+1 counter on it deals combat damage to a player, you may draw a card."

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE)
        ).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.May(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Karl Kopinski"
        flavorText = "Some see the world as a place of infinite wonder and knowledge. Some see it as an infinite dinner plate."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/4258a536-2275-45e8-8833-e921ca15c5a7.jpg"
    }
}
