package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Stoneforge Acolyte
 * {W}
 * Creature — Kor Artificer Ally
 * 1/2
 * Cohort — {T}, Tap an untapped Ally you control: Look at the top four cards of your library. You
 * may reveal an Equipment card from among them and put it into your hand. Put the rest on the
 * bottom of your library in any order.
 *
 * The dig is the shared [Patterns.Library.lookAtTopRevealMatchingToHand] recipe (Glint-Nest Crane's
 * shape) behind the cohort cost; "an Equipment card" is a bare Equipment-subtype filter.
 */
val StoneforgeAcolyte = card("Stoneforge Acolyte") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Artificer Ally"
    power = 1
    toughness = 2
    oracleText = "Cohort — {T}, Tap an untapped Ally you control: Look at the top four cards of your library. You may reveal an Equipment card from among them and put it into your hand. Put the rest on the bottom of your library in any order."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapPermanents(1, GameObjectFilter.Permanent.withSubtype(Subtype.ALLY)),
        )
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 4,
            filter = GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT),
            prompt = "You may reveal an Equipment card from among them and put it into your hand",
            restOrder = CardOrder.ControllerChooses
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Chris Rallis"
        flavorText = "\"The stone speaks. We are merely interpreters.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5b1ec11-48c3-49d8-9dcd-1da5ffaf9184.jpg"
    }
}
