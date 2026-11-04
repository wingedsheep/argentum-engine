package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Release to the Wind
 * {2}{U}
 * Instant
 *
 * Exile target nonland permanent. For as long as that card remains exiled, its owner may cast
 * it without paying its mana cost.
 *
 * Both halves of the permission go to the exiled card's *owner* (`ownerControls`): the may-cast
 * grant and the mana-cost waiver. `MayPlayExpiry.Permanent` is "for as long as that card remains
 * exiled" — a card that leaves exile is a new object and drops out of the grant (CR 400.7).
 * "Cast" rather than "play": a nonland permanent can't be a land, but `nonLandOnly` keeps a
 * modal double-faced card's land back face from being played through it.
 */
val ReleaseToTheWind = card("Release to the Wind") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Exile target nonland permanent. For as long as that card remains exiled, its owner " +
        "may cast it without paying its mana cost."

    spell {
        target(TargetFilter.NonlandPermanent)
        effect = Effects.Pipeline {
            val released = gather(CardSource.ChosenTargets)
            exile(released)
            run(Effects.GrantMayPlayFromExile(
                from = released,
                expiry = MayPlayExpiry.Permanent,
                nonLandOnly = true,
                ownerControls = true
            ))
            run(Effects.GrantPlayWithoutPayingCost(
                from = released,
                expiry = MayPlayExpiry.Permanent,
                ownerControls = true
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Joseph Meehan"
        flavorText = "\"Here and gone like a fleeting breeze.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b4f72ef-3939-467c-8fe4-fd1c215f2b1e.jpg?1783935323"
    }
}
