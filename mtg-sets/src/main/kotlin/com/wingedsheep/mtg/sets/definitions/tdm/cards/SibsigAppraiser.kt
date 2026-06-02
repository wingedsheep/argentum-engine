package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Sibsig Appraiser — Tarkir: Dragonstorm #56
 * {2}{U} · Creature — Zombie Advisor · 2/1
 *
 * When this creature enters, look at the top two cards of your library. Put one of them into
 * your hand and the other into your graveyard.
 *
 * Exactly the [LibraryPatterns.lookAtTopAndKeep] shape: count = 2, keepCount = 1. The kept card
 * goes to HAND and the remainder to the GRAVEYARD (the pattern defaults), matching oracle text.
 */
val SibsigAppraiser = card("Sibsig Appraiser") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Advisor"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, look at the top two cards of your library. Put one " +
        "of them into your hand and the other into your graveyard."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.lookAtTopAndKeep(count = 2, keepCount = 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Ina Wong"
        flavorText = "\"Oh, I can absolutely verify its authenticity,\" chuckled Sarnai. " +
            "\"I was there when it was made.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/670c5b96-bac6-449b-a2bd-cb43750d3911.jpg?1743204185"
    }
}
