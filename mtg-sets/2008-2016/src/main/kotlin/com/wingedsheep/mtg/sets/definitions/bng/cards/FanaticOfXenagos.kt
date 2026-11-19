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
 * Fanatic of Xenagos
 * {1}{R}{G}
 * Creature — Centaur Warrior
 * 3/3
 *
 * Trample
 * Tribute 1 (As this creature enters, an opponent of your choice may put a +1/+1 counter on it.)
 * When this creature enters, if tribute wasn't paid, it gets +1/+1 and gains haste until end of turn.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val FanaticOfXenagos = card("Fanatic of Xenagos") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Centaur Warrior"
    power = 3
    toughness = 3
    oracleText = "Trample\n" +
        "Tribute 1 (As this creature enters, an opponent of your choice may put a +1/+1 counter on it.)\n" +
        "When this creature enters, if tribute wasn't paid, it gets +1/+1 and gains haste until end of turn."

    keywords(Keyword.TRAMPLE)
    tribute(1)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.TributeNotPaid
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3687799c-81bd-4c49-902a-2a96863629c3.jpg?1783939526"
        ruling("2017-04-18", "For effects that check which player put counters on the entering creature, the player chosen to pay tribute puts those counters on it, not the creature's controller.")
        ruling("2014-02-01", "If the opponent pays tribute, the creature will enter the battlefield with the specified number of +1/+1 counters on it. It won't enter the battlefield and then have the +1/+1 counters placed on it.")
        ruling("2014-02-01", "Players can't respond to the tribute decision before the creature enters the battlefield. That is, if the opponent doesn't pay tribute, the triggered ability will trigger before any player has a chance to remove the creature.")
        ruling("2014-02-01", "The triggered ability will resolve even if the creature with tribute isn't on the battlefield at that time.")
    }
}
