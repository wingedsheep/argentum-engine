package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.tribute
import com.wingedsheep.sdk.model.Rarity

/**
 * Ornitharch
 * {3}{W}{W}
 * Creature — Archon
 * 3/3
 *
 * Flying
 * Tribute 2 (As this creature enters, an opponent of your choice may put two +1/+1 counters on it.)
 * When this creature enters, if tribute wasn't paid, create two 1/1 white Bird creature tokens with flying.
 *
 * Canonical printing: Born of the Gods, the card's only printing.
 */
val Ornitharch = card("Ornitharch") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Archon"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Tribute 2 (As this creature enters, an opponent of your choice may put two +1/+1 counters on it.)\n" +
        "When this creature enters, if tribute wasn't paid, create two 1/1 white Bird creature tokens with flying."

    keywords(Keyword.FLYING)

    tribute(2)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.TributeNotPaid
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/c/8/c862470b-f273-4e01-945b-bf3697cd1359.jpg?1783939516",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f95a0bf4-10c7-4afa-8cf6-e31196cc2bd5.jpg?1783939576"
        ruling("2017-04-18", "For effects that check which player put counters on the entering creature, the player chosen to pay tribute puts those counters on it, not the creature's controller.")
        ruling("2014-02-01", "If the opponent pays tribute, the creature will enter the battlefield with the specified number of +1/+1 counters on it. It won't enter the battlefield and then have the +1/+1 counters placed on it.")
        ruling("2014-02-01", "The choice of whether to pay tribute is made as the creature with tribute is entering the battlefield. At that point, it's too late to respond to the creature spell. For example, in a multiplayer game, opponents won't know whether tribute will be paid or which opponent will be chosen to pay tribute or not when deciding whether to counter the creature spell.")
        ruling("2014-02-01", "Players can't respond to the tribute decision before the creature enters the battlefield. That is, if the opponent doesn't pay tribute, the triggered ability will trigger before any player has a chance to remove the creature.")
        ruling("2014-02-01", "The triggered ability will resolve even if the creature with tribute isn't on the battlefield at that time.")
    }
}
