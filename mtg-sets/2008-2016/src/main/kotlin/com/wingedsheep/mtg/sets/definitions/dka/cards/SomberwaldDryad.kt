package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Somberwald Dryad
 * {1}{G}
 * Creature — Dryad
 * 2/2
 * Forestwalk
 */
val SomberwaldDryad = card("Somberwald Dryad") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dryad"
    oracleText = "Forestwalk (This creature can't be blocked as long as defending player controls a Forest.)"
    power = 2
    toughness = 2
    keywords(Keyword.FORESTWALK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Jaime Jones"
        flavorText = "The Somberwald is her domain, and she has yet to hear an acceptable excuse for trespassing."
        imageUri = "https://cards.scryfall.io/normal/front/3/0/307edca0-769d-4071-9654-3537341e96bd.jpg"
    }
}
