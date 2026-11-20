package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.tribute
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Siren of the Fanged Coast
 * {3}{U}{U}
 * Creature — Siren
 * 1/1
 *
 * Flying
 * Tribute 3 (As this creature enters, an opponent of your choice may put three +1/+1 counters on it.)
 * When this creature enters, if tribute wasn't paid, gain control of target creature.
 *
 * Canonical printing: Born of the Gods, the card's only printing.
 */
val SirenOfTheFangedCoast = card("Siren of the Fanged Coast") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Siren"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "Tribute 3 (As this creature enters, an opponent of your choice may put three +1/+1 counters on it.)\n" +
        "When this creature enters, if tribute wasn't paid, gain control of target creature."

    keywords(Keyword.FLYING)

    tribute(3)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.TributeNotPaid
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainControl(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Michael C. Hayes"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5ba93d9a-94cd-4d90-860a-f567793a5be4.jpg?1783939566"
        ruling("2017-04-18", "For effects that check which player put counters on the entering creature, the player chosen to pay tribute puts those counters on it, not the creature's controller.")
        ruling("2014-02-01", "If the opponent pays tribute, the creature will enter the battlefield with the specified number of +1/+1 counters on it. It won't enter the battlefield and then have the +1/+1 counters placed on it.")
        ruling("2014-02-01", "The choice of whether to pay tribute is made as the creature with tribute is entering the battlefield. At that point, it's too late to respond to the creature spell. For example, in a multiplayer game, opponents won't know whether tribute will be paid or which opponent will be chosen to pay tribute or not when deciding whether to counter the creature spell.")
        ruling("2014-02-01", "If the triggered ability has a target, that target will not be known while the creature spell with tribute is on the stack.")
        ruling("2014-02-01", "Players can't respond to the tribute decision before the creature enters the battlefield. That is, if the opponent doesn't pay tribute, the triggered ability will trigger before any player has a chance to remove the creature.")
        ruling("2014-02-01", "The triggered ability will resolve even if the creature with tribute isn't on the battlefield at that time.")
    }
}
