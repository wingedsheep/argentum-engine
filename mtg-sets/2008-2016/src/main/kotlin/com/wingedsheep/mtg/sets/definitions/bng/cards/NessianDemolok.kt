package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.tribute
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nessian Demolok
 * {3}{G}{G}
 * Creature — Beast
 * 3/3
 *
 * Tribute 3 (As this creature enters, an opponent of your choice may put three +1/+1 counters on it.)
 * When this creature enters, if tribute wasn't paid, destroy target noncreature permanent.
 *
 * Canonical printing: Born of the Gods, the card's only printing.
 */
val NessianDemolok = card("Nessian Demolok") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 3
    oracleText = "Tribute 3 (As this creature enters, an opponent of your choice may put three +1/+1 counters on it.)\n" +
        "When this creature enters, if tribute wasn't paid, destroy target noncreature permanent."

    tribute(3)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.TributeNotPaid
        val permanent = target(TargetFilter.NoncreaturePermanent)
        effect = Effects.Destroy(permanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee0683b2-8bc2-4c6a-964e-b909693b68c1.jpg?1783939533"
        ruling("2017-04-18", "For effects that check which player put counters on the entering creature, the player chosen to pay tribute puts those counters on it, not the creature's controller.")
        ruling("2014-02-01", "If the opponent pays tribute, the creature will enter the battlefield with the specified number of +1/+1 counters on it. It won't enter the battlefield and then have the +1/+1 counters placed on it.")
        ruling("2014-02-01", "If the triggered ability has a target, that target will not be known while the creature spell with tribute is on the stack.")
        ruling("2014-02-01", "Players can't respond to the tribute decision before the creature enters the battlefield. That is, if the opponent doesn't pay tribute, the triggered ability will trigger before any player has a chance to remove the creature.")
        ruling("2014-02-01", "The triggered ability will resolve even if the creature with tribute isn't on the battlefield at that time.")
    }
}
