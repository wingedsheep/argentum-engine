package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Skalla Wolf — Core Set 2019 #303 (canonical printing)
 * {3}{G}{G}
 * Creature — Wolf Spirit
 * 3/3
 * When this creature enters, look at the top five cards of your library. You may reveal a green
 * card from among them and put it into your hand. Put the rest on the bottom of your library in a
 * random order.
 *
 * The dig is the shared [Patterns.Library.lookAtTopRevealMatchingToHand] recipe; its default
 * remainder (library bottom, random order) is exactly the printed text.
 */
val SkallaWolf = card("Skalla Wolf") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf Spirit"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, look at the top five cards of your library. You may reveal a " +
        "green card from among them and put it into your hand. Put the rest on the bottom of your library " +
        "in a random order."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 5,
            filter = GameObjectFilter.Any.withColor(Color.GREEN),
            prompt = "You may reveal a green card from among them and put it into your hand",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "303"
        artist = "Lius Lasahido"
        flavorText = "\"This imprint was the familiar of a friend.\"\n—Vivien Reid"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f320e9f2-963d-468a-bf7a-726ef11e886d.jpg?1783934481"
    }
}
