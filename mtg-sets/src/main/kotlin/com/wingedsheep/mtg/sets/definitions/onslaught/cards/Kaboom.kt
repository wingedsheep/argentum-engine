package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RevealUntilNonlandDealDamageEachTargetEffect
import com.wingedsheep.sdk.targeting.TargetPlayer

/**
 * Kaboom!
 * {4}{R}
 * Sorcery
 * Choose any number of target players or planeswalkers. For each of them, reveal cards from
 * the top of your library until you reveal a nonland card, Kaboom! deals damage equal to that
 * card's mana value to that player or planeswalker, then you put the revealed cards on the
 * bottom of your library in any order.
 */
val Kaboom = card("Kaboom!") {
    manaCost = "{4}{R}"
    typeLine = "Sorcery"
    oracleText = "Choose any number of target players or planeswalkers. For each of them, reveal cards from the top of your library until you reveal a nonland card, Kaboom! deals damage equal to that card's mana value to that player or planeswalker, then you put the revealed cards on the bottom of your library in any order."

    spell {
        target = TargetPlayer(count = 99, optional = true)
        effect = RevealUntilNonlandDealDamageEachTargetEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Glen Angus"
        flavorText = "\"Blow it all up? But it's so pretty!\"\n—Skirk prospector"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e81e5fc-0e18-4dd8-a505-aa7dba8521a8.jpg"
    }
}
