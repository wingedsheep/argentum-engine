package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.tribute
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nessian Wilds Ravager
 * {4}{G}{G}
 * Creature — Hydra
 * 6/6
 *
 * Tribute 6 (As this creature enters, an opponent of your choice may put six +1/+1 counters on it.)
 * When this creature enters, if tribute wasn't paid, you may have this creature fight another target
 * creature. (Each deals damage equal to its power to the other.)
 *
 * Canonical printing: Born of the Gods, the card's only real-expansion printing.
 */
val NessianWildsRavager = card("Nessian Wilds Ravager") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hydra"
    power = 6
    toughness = 6
    oracleText = "Tribute 6 (As this creature enters, an opponent of your choice may put six +1/+1 counters on it.)\n" +
        "When this creature enters, if tribute wasn't paid, you may have this creature fight another target creature. " +
        "(Each deals damage equal to its power to the other.)"

    tribute(6)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.TributeNotPaid
        val other = target(TargetFilter.OtherCreature)
        optional = true
        effect = Effects.Fight(EffectTarget.Self, other)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "129"
        artist = "Richard Wright"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65368cf0-7d92-4248-b930-633fd023065b.jpg?1783939532"
        ruling("2017-04-18", "For effects that check which player put counters on the entering creature, the player chosen to pay tribute puts those counters on it, not the creature's controller.")
        ruling("2014-02-01", "If the opponent pays tribute, the creature will enter the battlefield with the specified number of +1/+1 counters on it. It won't enter the battlefield and then have the +1/+1 counters placed on it.")
        ruling("2014-02-01", "If the triggered ability has a target, that target will not be known while the creature spell with tribute is on the stack.")
        ruling("2014-02-01", "Players can't respond to the tribute decision before the creature enters the battlefield. That is, if the opponent doesn't pay tribute, the triggered ability will trigger before any player has a chance to remove the creature.")
    }
}
