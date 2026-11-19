package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Nessian Game Warden
 * {3}{G}{G}
 * Creature — Beast
 * 4/5
 *
 * When this creature enters, look at the top X cards of your library, where X is the number of
 * Forests you control. You may reveal a creature card from among them and put it into your hand.
 * Put the rest on the bottom of your library in any order.
 *
 * Bond of Flourishing's reveal-to-hand recipe with Kalonian Twingrove's Forest count, evaluated
 * as the trigger resolves.
 */
val NessianGameWarden = card("Nessian Game Warden") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 5
    oracleText = "When this creature enters, look at the top X cards of your library, where X is the number " +
        "of Forests you control. You may reveal a creature card from among them and put it into your hand. " +
        "Put the rest on the bottom of your library in any order."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = DynamicAmounts.landsWithSubtype(Subtype.FOREST),
            filter = GameObjectFilter.Creature,
            prompt = "You may reveal a creature card from among them and put it into your hand",
            restOrder = CardOrder.ControllerChooses
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5099d18d-c8b5-4706-bc93-40d1bb12988d.jpg?1783939410"
        ruling("2014-04-26", "The value of X is calculated as the ability resolves.")
    }
}
