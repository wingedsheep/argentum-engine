package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.tribute
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Shrike Harpy
 * {3}{B}{B}
 * Creature — Harpy
 * 2/2
 *
 * Flying
 * Tribute 2 (As this creature enters, an opponent of your choice may put two +1/+1 counters on it.)
 * When this creature enters, if tribute wasn't paid, target opponent sacrifices a creature of their choice.
 *
 * Canonical printing: Born of the Gods, the card's only printing.
 */
val ShrikeHarpy = card("Shrike Harpy") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Harpy"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Tribute 2 (As this creature enters, an opponent of your choice may put two +1/+1 counters on it.)\n" +
        "When this creature enters, if tribute wasn't paid, target opponent sacrifices a creature of their choice."

    keywords(Keyword.FLYING)

    tribute(2)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.TributeNotPaid
        val opponent = target(Targets.Opponent)
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, opponent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c2b1eeb-6cc9-48a7-a068-afa1011c45f2.jpg"
        ruling("2017-04-18", "For effects that check which player put counters on the entering creature, the player chosen to pay tribute puts those counters on it, not the creature's controller.")
        ruling("2014-02-01", "If the opponent pays tribute, the creature will enter the battlefield with the specified number of +1/+1 counters on it. It won't enter the battlefield and then have the +1/+1 counters placed on it.")
        ruling("2014-02-01", "If the triggered ability has a target, that target will not be known while the creature spell with tribute is on the stack.")
        ruling("2014-02-01", "Players can't respond to the tribute decision before the creature enters the battlefield. That is, if the opponent doesn't pay tribute, the triggered ability will trigger before any player has a chance to remove the creature.")
        ruling("2014-02-01", "The triggered ability will resolve even if the creature with tribute isn't on the battlefield at that time.")
    }
}
