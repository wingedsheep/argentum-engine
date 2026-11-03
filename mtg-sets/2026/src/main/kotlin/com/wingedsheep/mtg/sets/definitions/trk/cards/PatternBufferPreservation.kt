package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.beamMeUp
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Pattern Buffer Preservation
 * {1}{B}
 * Sorcery
 * Look at the top two cards of your library. Put one of them into your hand and the other into
 * your graveyard.
 * Beam me up {1}{B} (You may cast this card from your graveyard for {1}{B} if you also return a
 * creature you control to its owner's hand. Then exile this spell.)
 */
val PatternBufferPreservation = card("Pattern Buffer Preservation") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Look at the top two cards of your library. Put one of them into your hand and the " +
        "other into your graveyard.\n" +
        "Beam me up {1}{B} (You may cast this card from your graveyard for {1}{B} if you also " +
        "return a creature you control to its owner's hand. Then exile this spell.)"
    beamMeUp("{1}{B}")
    spell {
        effect = Patterns.Library.lookAtTopAndKeep(count = 2, keepCount = 1)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Alex Negrea"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23166580-9676-4025-916b-864568c2e59d.jpg?1793361304"
    }
}
