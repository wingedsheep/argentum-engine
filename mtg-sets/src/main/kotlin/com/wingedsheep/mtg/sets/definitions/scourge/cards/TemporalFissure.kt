package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Temporal Fissure
 * {4}{U}
 * Sorcery
 * Return target permanent to its owner's hand.
 * Storm (When you cast this spell, copy it for each spell cast before it this turn.
 * You may choose new targets for the copies.)
 */
val TemporalFissure = card("Temporal Fissure") {
    manaCost = "{4}{U}"
    typeLine = "Sorcery"
    oracleText = "Return target permanent to its owner's hand.\nStorm (When you cast this spell, copy it for each spell cast before it this turn. You may choose new targets for the copies.)"

    spell {
        val t = target("target permanent", Targets.Permanent)
        effect = Effects.ReturnToHand(t)
    }

    keywords(Keyword.STORM)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Edward P. Beard, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97949c53-aef7-4c0c-b846-8d4003193ced.jpg?1562532561"
    }
}
