package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Atomize
 * {2}{B}{G}
 * Instant
 *
 * Destroy target nonland permanent. Proliferate.
 */
val Atomize = card("Atomize") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Instant"
    oracleText = "Destroy target nonland permanent. Proliferate. (Choose any number of permanents " +
        "and/or players, then give each another counter of each kind already there.)"

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.Destroy(permanent) then Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Josu Solano"
        flavorText = "Plasma rifles—separating soft tissues from hard ones since 2077."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/592cbafc-ab4e-461f-b64f-0bf9d83aaff3.jpg?1783912392"
    }
}
