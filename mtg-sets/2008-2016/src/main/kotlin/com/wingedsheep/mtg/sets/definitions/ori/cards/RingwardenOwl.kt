package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ringwarden Owl
 * {3}{U}{U}
 * Creature — Bird
 * 3/3
 * Flying
 * Prowess
 */
val RingwardenOwl = card("Ringwarden Owl") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird"
    power = 3
    toughness = 3
    oracleText = "Flying (This creature can't be blocked except by creatures with flying or reach.)\nProwess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)"

    keywords(Keyword.FLYING, Keyword.PROWESS)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Titus Lunter"
        flavorText = "The owls learn of mana from the mages who know it best."
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1acf216d-ef8f-431b-9b65-1e2e91285517.jpg?1783938348"

        ruling("2015-06-22", "Prowess goes on the stack on top of the spell that caused it to trigger. It will resolve before that spell.")
        ruling("2015-06-22", "Any spell you cast that doesn't have the type creature will cause prowess to trigger. If a spell has multiple types, and one of those types is creature (such as an artifact creature), casting it won't cause prowess to trigger. Playing a land also won't cause prowess to trigger.")
        ruling("2015-06-22", "Once it triggers, prowess isn't connected to the spell that caused it to trigger. If that spell is countered, prowess will still resolve.")
    }
}
