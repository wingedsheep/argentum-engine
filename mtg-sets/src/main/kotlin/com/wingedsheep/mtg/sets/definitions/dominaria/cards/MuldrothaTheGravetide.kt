package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard

/**
 * Muldrotha, the Gravetide
 * {3}{B}{G}{U}
 * Legendary Creature — Elemental Avatar
 * 6/6
 * During each of your turns, you may play a land and cast a permanent spell of each
 * permanent type from your graveyard. (If a card has multiple permanent types, choose
 * one as you play it.)
 */
val MuldrothaTheGravetide = card("Muldrotha, the Gravetide") {
    manaCost = "{3}{B}{G}{U}"
    typeLine = "Legendary Creature — Elemental Avatar"
    power = 6
    toughness = 6
    oracleText = "During each of your turns, you may play a land and cast a permanent spell of each permanent type from your graveyard. (If a card has multiple permanent types, choose one as you play it.)"

    staticAbility {
        ability = MayPlayPermanentsFromGraveyard
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "199"
        artist = "Jason Rainville"
        flavorText = "\"My child grew from rot and ruin, yet she bloomed.\"\n—Multani"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c654737d-34ac-42ff-ae27-3a3bbb930fc1.jpg?1591204580"
        ruling("2018-04-27", "You must pay all costs and follow all normal timing rules for cards played this way. For example, you may only play a land this way during your main phase while the stack is empty.")
        ruling("2018-04-27", "If you play a card from your graveyard and then have a new Muldrotha come under your control in the same turn, you may play another card of that type from your graveyard that turn.")
        ruling("2018-04-27", "If a permanent card has multiple types, choose which type's permission it uses as you play it.")
        ruling("2020-11-10", "If multiple effects allow you to play a card from your graveyard, you must announce which permission you're using as you begin to play the card.")
    }
}
