package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Daring Buccaneer
 * {R}
 * Creature — Human Pirate
 * 2/2
 * As an additional cost to cast this spell, reveal a Pirate card from your hand or pay {2}.
 *
 * The filter is any Pirate *card* — the revealed card stays in hand and can still be cast.
 */
val DaringBuccaneer = card("Daring Buccaneer") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 2
    oracleText = "As an additional cost to cast this spell, reveal a Pirate card from your hand or pay {2}."

    additionalCost(
        Costs.additional.RevealFromHandOrPay(
            filter = GameObjectFilter.Any.withSubtype(Subtype.PIRATE),
            alternativeManaCost = "{2}"
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "98"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d413b88-8f5d-415f-bd07-169f6c175e6b.jpg?1783935300"
    }
}
