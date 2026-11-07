package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Courageous Outrider — Eldritch Moon #18
 * {3}{W} · Creature — Human Scout · 3/4
 *
 * When this creature enters, look at the top four cards of your library. You may reveal a Human
 * card from among them and put it into your hand. Put the rest on the bottom of your library in
 * any order.
 *
 * Glint-Nest Crane's dig over a subtype: [Patterns.Library.lookAtTopRevealMatchingToHand] with a
 * Human filter on any card type, and [CardOrder.ControllerChooses] for "in any order".
 */
val CourageousOutrider = card("Courageous Outrider") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Scout"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, look at the top four cards of your library. You may reveal " +
        "a Human card from among them and put it into your hand. Put the rest on the bottom of your " +
        "library in any order."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 4,
            filter = GameObjectFilter.Any.withSubtype(Subtype.HUMAN),
            prompt = "You may reveal a Human card from among them and put it into your hand",
            restOrder = CardOrder.ControllerChooses
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "18"
        artist = "Ryan Alexander Lee"
        flavorText = "One silvery blast signaled that she wouldn't be arriving at Thraben alone."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4123da54-9947-462e-9862-3eecc459a75b.jpg"
    }
}
