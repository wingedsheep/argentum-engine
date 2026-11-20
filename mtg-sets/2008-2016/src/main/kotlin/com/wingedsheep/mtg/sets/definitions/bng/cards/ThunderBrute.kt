package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.tribute
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thunder Brute
 * {4}{R}{R}
 * Creature — Cyclops
 * 5/5
 *
 * Trample
 * Tribute 3 (As this creature enters, an opponent of your choice may put three +1/+1 counters on it.)
 * When this creature enters, if tribute wasn't paid, it gains haste until end of turn.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val ThunderBrute = card("Thunder Brute") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Cyclops"
    power = 5
    toughness = 5
    oracleText = "Trample\n" +
        "Tribute 3 (As this creature enters, an opponent of your choice may put three +1/+1 counters on it.)\n" +
        "When this creature enters, if tribute wasn't paid, it gains haste until end of turn."

    keywords(Keyword.TRAMPLE)
    tribute(3)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.TributeNotPaid
        effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Phill Simmer"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c28fa4f8-4b87-41dc-802d-dfd160ccf1a8.jpg?1783939539"
        ruling("2017-04-18", "For effects that check which player put counters on the entering creature, the player chosen to pay tribute puts those counters on it, not the creature's controller.")
        ruling("2014-02-01", "If the opponent pays tribute, the creature will enter the battlefield with the specified number of +1/+1 counters on it. It won't enter the battlefield and then have the +1/+1 counters placed on it.")
        ruling("2014-02-01", "Players can't respond to the tribute decision before the creature enters the battlefield. That is, if the opponent doesn't pay tribute, the triggered ability will trigger before any player has a chance to remove the creature.")
        ruling("2014-02-01", "The triggered ability will resolve even if the creature with tribute isn't on the battlefield at that time.")
    }
}
