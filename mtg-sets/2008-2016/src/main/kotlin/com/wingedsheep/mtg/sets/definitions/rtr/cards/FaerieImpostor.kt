package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Faerie Impostor — Return to Ravnica #39
 * {U} · Creature — Faerie Rogue · 2/1
 *
 * Flying
 * When this creature enters, sacrifice it unless you return another creature you control to its
 * owner's hand.
 *
 * Quickling's shape. The resolution-time return cost never offers its own source, which is the
 * printed "another"; paying it is optional (2012-10-01 ruling).
 */
val FaerieImpostor = card("Faerie Impostor") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Rogue"
    power = 2
    toughness = 1
    oracleText = "Flying\nWhen this creature enters, sacrifice it unless you return another creature you control to its owner's hand."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.ReturnToHand(filter = GameObjectFilter.Creature),
            suffer = SacrificeSelfEffect
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "39"
        artist = "Johann Bodin"
        flavorText = "Many Tin Street shops display a sign on the door: \"No cloaks allowed.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7dfddc9d-85f6-4b37-9973-14c69f6818ec.jpg?1783940370"
        ruling(
            "2012-10-01",
            "Returning another creature you control to its owner's hand is optional. You may choose to sacrifice Faerie Impostor, even if you could return a creature."
        )
    }
}
