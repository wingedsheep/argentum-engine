package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Shipwreck Looter
 * {1}{U}
 * Creature — Human Pirate
 * 2/1
 * Raid — When this creature enters, if you attacked this turn, you may draw a card. If you do,
 * discard a card.
 *
 * Raid is the intervening-if [Conditions.YouAttackedThisTurn] on the enters trigger, as on
 * [StormFleetSpy]. "You may draw a card. If you do, discard a card." is the coupled loot —
 * [Effects.May] over `Patterns.Hand.loot()`, so declining draws nothing and discards nothing.
 */
val ShipwreckLooter = card("Shipwreck Looter") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 1
    oracleText = "Raid — When this creature enters, if you attacked this turn, you may draw a card. " +
        "If you do, discard a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouAttackedThisTurn
        effect = Effects.May(Patterns.Hand.loot())
        description = "Raid — When this creature enters, if you attacked this turn, you may draw a card. " +
            "If you do, discard a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Wayne Reynolds"
        flavorText = "One pirate's wreck is another's reward."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/891cbde7-0dff-4877-b373-03f9f4332002.jpg?1783935773"
    }
}
