package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Run Out of Town
 * {3}{U}
 * Instant
 *
 * The owner of target nonland permanent puts it on their choice of the top or bottom of their library.
 */
val RunOutOfTown = card("Run Out of Town") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText =
        "The owner of target nonland permanent puts it on their choice of the top or bottom of their library."

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.PutOnTopOrBottomOfLibrary(permanent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "Lucas Staniec"
        flavorText = "Better to depart in haste than be dearly departed."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cf00112-0844-49db-a061-d0eb488de68a.jpg?1783923139"
    }
}
