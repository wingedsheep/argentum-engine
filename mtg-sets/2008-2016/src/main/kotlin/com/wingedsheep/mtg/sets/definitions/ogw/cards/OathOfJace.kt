package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Oath of Jace
 * {2}{U}
 * Legendary Enchantment
 * When Oath of Jace enters, draw three cards, then discard two cards.
 * At the beginning of your upkeep, scry X, where X is the number of planeswalkers you control.
 *
 * X is counted as the upkeep trigger resolves. With no planeswalkers it is "scry 0": no look, no
 * decision, and no scry event, so "whenever you scry" triggers stay quiet — the dynamic
 * [Effects.Scry] pipeline gates its scried-event tail on X > 0.
 */
val OathOfJace = card("Oath of Jace") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Enchantment"
    oracleText = "When Oath of Jace enters, draw three cards, then discard two cards.\n" +
        "At the beginning of your upkeep, scry X, where X is the number of planeswalkers you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(3) then Effects.Discard(2)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Scry(DynamicAmounts.battlefield(Player.You, GameObjectFilter.Planeswalker).count())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Wesley Burt"
        flavorText = "\"For the sake of the Multiverse, I will keep watch.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8e7b007-a14b-40b2-9de7-38545c50a073.jpg?1783937918"
        ruling("2016-01-22", "If you control no planeswalkers as the last ability resolves, you won’t scry at all. Abilities that trigger whenever you scry won’t trigger.")
        ruling("2016-01-22", "Each of the Oaths is legendary: if you control more than one of any particular Oath, you choose one to remain on the battlefield and the others are put into your graveyard.")
        ruling("2016-01-22", "The “legend rule” checks the full names of legendary permanents. You can control Oath of Gideon and Oath of Chandra at the same time, for example.")
        ruling("2016-01-22", "Even if you control a particular Oath, you can cast another of that Oath to take advantage of its enters-the-battlefield ability. It will enter the battlefield, causing its first ability to trigger. You will then choose one to keep, as detailed above, and then its enters-the-battlefield ability will resolve.")
    }
}
