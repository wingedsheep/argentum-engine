package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Quickling — Magic 2015 #76
 * {1}{U} · Creature — Faerie Rogue · 2/2
 *
 * Flash
 * Flying
 * When this creature enters, sacrifice it unless you return another creature you control to its
 * owner's hand.
 *
 * Drake Familiar's shape. The resolution-time return cost never puts its own source in the pool,
 * which is exactly the printed "another"; with no other creature you control, it is unpayable and
 * Quickling is sacrificed (2014-07-18 ruling).
 */
val Quickling = card("Quickling") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Rogue"
    power = 2
    toughness = 2
    oracleText = "Flash\nFlying\nWhen this creature enters, sacrifice it unless you return another creature you control to its owner's hand."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.ReturnToHand(filter = GameObjectFilter.Creature),
            suffer = SacrificeSelfEffect
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/278fbdb9-51e3-4ed6-bcc5-c5b86f06c216.jpg?1783939188"
        ruling(
            "2014-07-18",
            "If you control no other creatures when the enters-the-battlefield ability resolves, you must sacrifice Quickling."
        )
    }
}
