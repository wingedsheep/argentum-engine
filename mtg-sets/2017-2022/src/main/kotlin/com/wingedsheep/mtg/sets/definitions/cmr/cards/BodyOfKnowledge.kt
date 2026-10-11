package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.NoMaximumHandSize

/**
 * Body of Knowledge
 * {3}{U}{U}
 * Creature — Avatar
 * * / *
 * Body of Knowledge's power and toughness are each equal to the number of cards in your hand.
 * You have no maximum hand size.
 * Whenever this creature is dealt damage, draw that many cards.
 */
val BodyOfKnowledge = card("Body of Knowledge") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Avatar"
    oracleText = "Body of Knowledge's power and toughness are each equal to the number of cards in your hand.\n" +
        "You have no maximum hand size.\n" +
        "Whenever this creature is dealt damage, draw that many cards."

    dynamicStats(DynamicAmounts.cardsInYourHand())

    staticAbility {
        ability = NoMaximumHandSize
    }

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.DrawCards(DynamicAmounts.triggerDamageAmount())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "59"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/786ae686-0790-4ba1-927f-523984331549.jpg?1783928867"
        ruling("2020-11-10", "The ability that defines Body of Knowledge's power and toughness applies in all zones, not just the battlefield.")
        ruling("2020-11-10", "As long as Body of Knowledge is in your hand, its ability will count itself.")
        ruling("2020-11-10", "Creatures may be dealt damage greater than their toughness. For example, if Body of Knowledge is a 3/3 creature and a source deals 5 damage to it, you'll draw five cards. Note that if Body of Knowledge is dealt lethal damage, it dies before the cards are drawn, meaning they won't raise Body of Knowledge's toughness in time to save it.")
    }
}
