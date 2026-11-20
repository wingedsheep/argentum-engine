package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Bred for the Hunt
 * {1}{G}{U}
 * Enchantment
 * Whenever a creature you control with a +1/+1 counter on it deals combat damage to a player,
 * you may draw a card.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 *
 * Granted-trigger shape (as Synapse Sliver): each creature you control with a +1/+1 counter carries
 * "whenever this deals combat damage to a player, you may draw a card"; its controller is you.
 */
val BredForTheHunt = card("Bred for the Hunt") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control with a +1/+1 counter on it deals combat damage to a player, you may draw a card."

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
                effect = Effects.May(Effects.DrawCards(1)),
            ),
            filter = GroupFilter(
                GameObjectFilter.Creature.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE)
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Karl Kopinski"
        flavorText = "Some see the world as a place of infinite wonder and knowledge. Some see it as an infinite dinner plate."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/4258a536-2275-45e8-8833-e921ca15c5a7.jpg"
    }
}
