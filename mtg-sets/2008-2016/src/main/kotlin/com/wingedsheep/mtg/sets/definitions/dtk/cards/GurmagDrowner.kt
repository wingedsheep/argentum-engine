package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.exploit
import com.wingedsheep.sdk.model.Rarity

/**
 * Gurmag Drowner
 * {3}{U}
 * Creature — Snake Wizard
 * 2/4
 * Exploit (When this creature enters, you may sacrifice a creature.)
 * When this creature exploits a creature, look at the top four cards of your library. Put one of
 * them into your hand and the rest into your graveyard.
 *
 * The payoff is [Patterns.Library.lookAtTopAndKeep] (keep one to hand, rest to the graveyard)
 * riding the exploit reflexive, so it still fires when the Drowner exploits itself.
 */
val GurmagDrowner = card("Gurmag Drowner") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Snake Wizard"
    power = 2
    toughness = 4
    oracleText = "Exploit (When this creature enters, you may sacrifice a creature.)\n" +
        "When this creature exploits a creature, look at the top four cards of your library. Put one of them into your hand and the rest into your graveyard."

    exploit(onExploit = Patterns.Library.lookAtTopAndKeep(count = 4, keepCount = 1))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Lake Hurwitz"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5087a664-6ed7-43a1-8851-71bbee204eb1.jpg"
    }
}
