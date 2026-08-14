package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Trip Up — Aetherdrift #71
 * {3}{U} · Instant
 *
 * Target nonland permanent's owner puts it on their choice of the top or bottom of their library.
 * Cycling {2}
 *
 * The top-or-bottom choice belongs to the permanent's *owner*, not to Trip Up's controller —
 * [Effects.PutOnTopOrBottomOfLibrary] pauses for exactly that player. Tokens are legal targets and
 * simply cease to exist once they leave the battlefield.
 */
val TripUp = card("Trip Up") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target nonland permanent's owner puts it on their choice of the top or bottom of " +
        "their library.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    spell {
        val permanent = target("target nonland permanent", Targets.NonlandPermanent)
        effect = Effects.PutOnTopOrBottomOfLibrary(permanent)
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "Josiah \"Jo\" Cameron"
        flavorText = "\"That net was made to hold skywhales. Good luck getting to the next checkpoint!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/7/273061f3-7aa7-4cb0-afd6-616252b88948.jpg?1783907901"
    }
}
