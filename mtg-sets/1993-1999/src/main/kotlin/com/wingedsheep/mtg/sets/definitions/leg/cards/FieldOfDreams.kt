package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.PlayersRevealTopOfLibrary

/**
 * Field of Dreams
 * {U}
 * World Enchantment
 *
 * Players play with the top card of their libraries revealed.
 *
 * The symmetric reveal is [PlayersRevealTopOfLibrary] (shared with Wizened Snitches): every
 * player's top card is public to every player, with no permission to play it from there.
 */
val FieldOfDreams = card("Field of Dreams") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "World Enchantment"
    oracleText = "Players play with the top card of their libraries revealed."

    staticAbility {
        ability = PlayersRevealTopOfLibrary
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Kaja Foglio"
        flavorText = "Some people say that the world is round, and if you travel far enough you'll come to " +
            "the other side, where everything is upside down."
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a63e119-3b1b-4964-a4b9-b10170ff542b.jpg?1783948076"
    }
}
