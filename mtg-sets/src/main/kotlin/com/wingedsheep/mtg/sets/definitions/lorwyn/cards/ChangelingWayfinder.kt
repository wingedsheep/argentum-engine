package com.wingedsheep.mtg.sets.definitions.lorwyn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.scripting.SearchLibraryEffect

/**
 * Changeling Wayfinder
 * {3}
 * Creature — Shapeshifter
 * 1/2
 * Changeling (This card is every creature type.)
 * When this creature enters, you may search your library for a basic land card,
 * reveal it, put it into your hand, then shuffle.
 */
val ChangelingWayfinder = card("Changeling Wayfinder") {
    manaCost = "{3}"
    typeLine = "Creature — Shapeshifter"
    power = 1
    toughness = 2

    keywords(Keyword.CHANGELING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        effect = SearchLibraryEffect(
            filter = CardFilter.BasicLandCard,
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Quintin Gleim"
        flavorText = "\"No map. No complaints.\""
    }
}
