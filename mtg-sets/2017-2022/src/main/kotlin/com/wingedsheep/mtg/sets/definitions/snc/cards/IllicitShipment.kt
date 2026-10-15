package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Illicit Shipment
 * {3}{B}{B}
 * Sorcery
 * Casualty 3 (As you cast this spell, you may sacrifice a creature with power 3 or greater. When you do, copy this spell.)
 * Search your library for a card, put it into your hand, then shuffle.
 *
 * Casualty 3 (CR 702.153) is the printed [KeywordAbility.Casualty]; paying it copies the tutor, so
 * one cast finds two cards. The tutor is the plain [Patterns.Library.searchLibrary] to hand.
 */
val IllicitShipment = card("Illicit Shipment") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Casualty 3 (As you cast this spell, you may sacrifice a creature with power 3 or greater. When you do, copy this spell.)\n" +
        "Search your library for a card, put it into your hand, then shuffle."

    keywordAbility(KeywordAbility.casualty(3))

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any,
            destination = SearchDestination.HAND
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Josu Hernaiz"
        flavorText = "Whatever the Maestros had ordered, it wasn't anyone's business, including his."
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f7cced2-390b-499f-ace3-e52fd53d4712.jpg"
    }
}
