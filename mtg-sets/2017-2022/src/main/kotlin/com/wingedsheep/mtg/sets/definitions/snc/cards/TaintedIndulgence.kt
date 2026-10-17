package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tainted Indulgence
 * {U}{B}
 * Instant
 *
 * Draw two cards. Then discard a card unless there are five or more mana values among cards in
 * your graveyard.
 *
 * The draw is unconditional; the discard is gated on the negation of
 * [Conditions.DistinctManaValuesInGraveyard], evaluated at resolution after the draw.
 */
val TaintedIndulgence = card("Tainted Indulgence") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Instant"
    oracleText = "Draw two cards. Then discard a card unless there are five or more mana values among cards in your graveyard."

    spell {
        effect = Effects.DrawCards(2) then Effects.If(
            condition = Conditions.Not(Conditions.DistinctManaValuesInGraveyard(5)),
            then = Effects.Discard(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Inka Schulz"
        flavorText = "Their toast to good health went immediately, horribly awry."
        imageUri = "https://cards.scryfall.io/normal/front/3/4/347d883c-84ab-4547-813b-f4385366fbce.jpg?1783923067"
    }
}
