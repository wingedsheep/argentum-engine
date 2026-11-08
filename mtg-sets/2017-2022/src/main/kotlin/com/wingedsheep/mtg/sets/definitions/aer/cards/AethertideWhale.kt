package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aethertide Whale
 * {4}{U}{U}
 * Creature — Whale
 * 6/4
 * Flying
 * When this creature enters, you get six {E} (energy counters).
 * Pay {E}{E}{E}{E}: Return this creature to its owner's hand.
 */
val AethertideWhale = card("Aethertide Whale") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Whale"
    power = 6
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, you get six {E} (energy counters).\n" +
        "Pay {E}{E}{E}{E}: Return this creature to its owner's hand."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(6)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 4)
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        description = "Pay {E}{E}{E}{E}: Return this creature to its owner's hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Steven Belledin"
        flavorText = "\"To survive up here, I've learned when to use my harpoon and when to just enjoy the view.\"\n" +
            "—Kadhu, skywhaler"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38722c25-13a7-47af-a4cd-90722f289499.jpg?1783936774"
    }
}
