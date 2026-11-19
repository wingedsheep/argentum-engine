package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.tribute
import com.wingedsheep.sdk.model.Rarity

/**
 * Snake of the Golden Grove
 * {4}{G}
 * Creature — Snake
 * 4/4
 *
 * Tribute 3 (As this creature enters, an opponent of your choice may put three +1/+1 counters on it.)
 * When this creature enters, if tribute wasn't paid, you gain 4 life.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val SnakeOfTheGoldenGrove = card("Snake of the Golden Grove") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake"
    power = 4
    toughness = 4
    oracleText = "Tribute 3 (As this creature enters, an opponent of your choice may put three +1/+1 counters on it.)\n" +
        "When this creature enters, if tribute wasn't paid, you gain 4 life."

    tribute(3)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.TributeNotPaid
        effect = Effects.GainLife(4)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Mathias Kollros"
        flavorText = "Some fruits are best left ungathered."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08b10bd7-3f8b-426f-acdf-5a9559b9bc7d.jpg?1783939527"
        ruling("2017-04-18", "For effects that check which player put counters on the entering creature, the player chosen to pay tribute puts those counters on it, not the creature's controller.")
        ruling("2014-02-01", "If the opponent pays tribute, the creature will enter the battlefield with the specified number of +1/+1 counters on it. It won't enter the battlefield and then have the +1/+1 counters placed on it.")
        ruling("2014-02-01", "The triggered ability will resolve even if the creature with tribute isn't on the battlefield at that time.")
    }
}
