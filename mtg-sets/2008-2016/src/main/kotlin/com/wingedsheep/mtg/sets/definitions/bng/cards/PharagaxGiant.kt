package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.tribute
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pharagax Giant
 * {4}{R}
 * Creature — Giant
 * 3/3
 *
 * Tribute 2 (As this creature enters, an opponent of your choice may put two +1/+1 counters on it.)
 * When this creature enters, if tribute wasn't paid, this creature deals 5 damage to each opponent.
 *
 * Canonical printing: Born of the Gods, the card's only printing.
 */
val PharagaxGiant = card("Pharagax Giant") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    power = 3
    toughness = 3
    oracleText = "Tribute 2 (As this creature enters, an opponent of your choice may put two +1/+1 counters on it.)\n" +
        "When this creature enters, if tribute wasn't paid, this creature deals 5 damage to each opponent."

    tribute(2)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.TributeNotPaid
        effect = Effects.DealDamage(5, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9eef1b9b-58f1-40af-a7c2-06cdaf1ec974.jpg?1783939543"
        ruling("2017-04-18", "For effects that check which player put counters on the entering creature, the player chosen to pay tribute puts those counters on it, not the creature's controller.")
        ruling("2014-02-01", "If the opponent pays tribute, the creature will enter the battlefield with the specified number of +1/+1 counters on it. It won't enter the battlefield and then have the +1/+1 counters placed on it.")
        ruling("2014-02-01", "Players can't respond to the tribute decision before the creature enters the battlefield. That is, if the opponent doesn't pay tribute, the triggered ability will trigger before any player has a chance to remove the creature.")
        ruling("2014-02-01", "The triggered ability will resolve even if the creature with tribute isn't on the battlefield at that time.")
    }
}
