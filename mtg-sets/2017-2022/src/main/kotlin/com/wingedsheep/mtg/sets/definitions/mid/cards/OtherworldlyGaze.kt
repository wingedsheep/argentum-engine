package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Otherworldly Gaze
 * {U}
 * Instant
 * Surveil 3.
 * Flashback {1}{U}
 */
val OtherworldlyGaze = card("Otherworldly Gaze") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Surveil 3. (Look at the top three cards of your library, then put any number of them " +
        "into your graveyard and the rest on top of your library in any order.)\n" +
        "Flashback {1}{U} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Effects.Surveil(3)
    }

    keywordAbility(KeywordAbility.flashback("{1}{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Chris Cold"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cebee20-869e-45ed-9ddc-843faf4032ad.jpg"
    }
}
