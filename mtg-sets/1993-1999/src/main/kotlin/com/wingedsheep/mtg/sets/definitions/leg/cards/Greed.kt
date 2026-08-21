package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Greed — Legends #101
 * {3}{B} · Enchantment
 *
 * {B}, Pay 2 life: Draw a card.
 *
 * Legends is Greed's earliest printing, so the canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives here; Commander 2013 and Modern Horizons 2 contribute `Printing` rows only.
 *
 * The life payment is part of the *cost*, not an effect: [Costs.PayLife] inside a
 * [Costs.Composite] alongside the mana, so activation is illegal unless the life can actually be
 * paid, and the draw never happens on a failed activation. Life payment is legal at any total
 * above zero — a player at 2 may pay and then lose to the state-based action.
 */
val Greed = card("Greed") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "{B}, Pay 2 life: Draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.PayLife(2))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Phil Foglio"
        flavorText = "\"There is no calamity greater than lavish desires./ There is no greater guilt than discontentment./ And there is no greater disaster than greed.\" —*Tao Tê Ching 46*"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/111a16a2-e875-4756-80db-290f9e8606db.jpg?1783948066"
    }
}
